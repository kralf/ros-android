/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.ros.android;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.ros.RosCore;
import org.ros.concurrent.ListenerGroup;
import org.ros.concurrent.SignalRunnable;
import org.ros.exception.RosRuntimeException;
import org.ros.node.DefaultNodeMainExecutor;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeListener;
import org.ros.node.NodeMain;
import org.ros.node.NodeMainExecutor;
import org.ros.node.Node;
import org.ros.node.ConnectedNode;
import org.ros.address.InetAddressFactory;
import org.ros.namespace.NameResolver;

import java.net.URI;
import java.util.Collection;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author damonkohler@google.com (Damon Kohler)
 * @author ralf.kaestner@gmail.com (Ralf Kaestner)
 */
public class NodeMainExecutorService
  extends Service
  implements NodeMainExecutor, NodeListener {

  private static final String TAG = "NodeMainExecutorService";

  // NOTE(damonkohler): If this is 0, the notification does not show up.
  private static final int SHUTDOWN_NOTIFICATION = 1;
  private static final int STATUS_NOTIFICATION = 2;

  public static final String ACTION_START = 
    "org.ros.android.ACTION_START_NODE_RUNNER_SERVICE";
  public static final String ACTION_SHUTDOWN = 
    "org.ros.android.ACTION_SHUTDOWN_NODE_RUNNER_SERVICE";
  public static final String EXTRA_NOTIFICATION_TITLE = 
    "org.ros.android.EXTRA_NOTIFICATION_TITLE";
  public static final String EXTRA_NOTIFICATION_TICKER =
    "org.ros.android.EXTRA_NOTIFICATION_TICKER";

  private final NodeMainExecutor nodeMainExecutor;
  private final IBinder binder;
  private final ListenerGroup<NodeMainExecutorServiceListener> listeners;

  private WakeLock wakeLock;
  private WifiLock wifiLock;
  private RosCore rosCore;
  private URI masterUri;
  private String namespace;
  
  private PowerManager powerManager;
  private NotificationManager notificationManager;
  
  private String notificationTitle;
  private int notificationSmallIcon;
  
  /**
   * Class for clients to access. Because we know this service always runs in
   * the same process as its clients, we don't need to deal with IPC.
   */
  class Binder extends android.os.Binder {
    NodeMainExecutorService getService() {
      return NodeMainExecutorService.this;
    }
  }
  
  public NodeMainExecutorService() {
    super();
    nodeMainExecutor = DefaultNodeMainExecutor.newDefault();
    binder = new Binder();
    listeners = new ListenerGroup<NodeMainExecutorServiceListener>(
      nodeMainExecutor.getScheduledExecutorService());
  }

  @Override
  public void onCreate() {
    powerManager = (PowerManager)getSystemService(POWER_SERVICE);
    notificationManager = (NotificationManager)getSystemService(
      NOTIFICATION_SERVICE);
    
    notificationSmallIcon = getApplicationContext().getApplicationInfo().icon;
    
    wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
    wakeLock.acquire();
    int wifiLockType = WifiManager.WIFI_MODE_FULL;
    try {
      wifiLockType = WifiManager.class.getField(
        "WIFI_MODE_FULL_HIGH_PERF").getInt(null);
    } catch (Exception e) {
      // We must be running on a pre-Honeycomb device.
      Log.w(TAG, "Unable to acquire high performance wifi lock.");
    }
    WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
    wifiLock = wifiManager.createWifiLock(wifiLockType, TAG);
    wifiLock.acquire();
  }

  @Override
  public void execute(NodeMain nodeMain, NodeConfiguration nodeConfiguration,
      Collection<NodeListener> nodeListeneners) {
    nodeMainExecutor.execute(nodeMain, nodeConfiguration, nodeListeneners);
  }

  @Override
  public void execute(NodeMain nodeMain, NodeConfiguration nodeConfiguration) {
    execute(nodeMain, nodeConfiguration, null);
  }

  @Override
  public ScheduledExecutorService getScheduledExecutorService() {
    return nodeMainExecutor.getScheduledExecutorService();
  }

  @Override
  public void shutdownNodeMain(NodeMain nodeMain) {
    nodeMainExecutor.shutdownNodeMain(nodeMain);
  }

  @Override
  public void shutdown() {
    listeners.signal(new SignalRunnable<NodeMainExecutorServiceListener>() {
      @Override
      public void run(NodeMainExecutorServiceListener
          nodeMainExecutorServiceListener) {
        nodeMainExecutorServiceListener.onShutdown(
          NodeMainExecutorService.this);
      }
    });
    
    // NOTE(damonkohler): This may be called multiple times. Shutting down a
    // NodeMainExecutor multiple times is safe. It simply calls shutdown on all
    // NodeMains.
    nodeMainExecutor.shutdown();
    if (rosCore != null) {
      rosCore.shutdown();
    }
    if (wakeLock.isHeld()) {
      wakeLock.release();
    }
    if (wifiLock.isHeld()) {
      wifiLock.release();
    }
    stopForeground(true);
    stopSelf();
  }

  public void connect(NodeMain nodeMain) {
    new AsyncTask<NodeMain, Void, Void>() {
      @Override
      protected Void doInBackground(NodeMain... params) {
        NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(
          InetAddressFactory.newNonLoopback().getHostAddress());
        nodeConfiguration.setMasterUri(masterUri);
        if (!namespace.isEmpty()) {
          NameResolver resolver = NameResolver.newFromNamespace(namespace);
          nodeConfiguration.setParentResolver(resolver);
        }
        
        Collection<NodeListener> listeners = Lists.newArrayList();
        listeners.add(NodeMainExecutorService.this);
        NodeMainExecutorService.this.execute(params[0], nodeConfiguration,
          listeners);
        
        return null;
      }
    }.execute(nodeMain);
  }

  public void disconnect(NodeMain nodeMain) {
    new AsyncTask<NodeMain, Void, Void>() {
      @Override
      protected Void doInBackground(NodeMain... params) {
        shutdownNodeMain(params[0]);
        return null;
      }
    }.execute(nodeMain);
  }

  public void disconnect() {
    shutdown();
  }
  
  public void addListener(NodeMainExecutorServiceListener listener) {
    listeners.add(listener);
  }

  @Override
  public void onDestroy() {
    shutdown();
    super.onDestroy();
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    if (intent.getAction() == null) {
      return START_NOT_STICKY;
    }
    else if (intent.getAction().equals(ACTION_START)) {
      Preconditions.checkArgument(intent.hasExtra(EXTRA_NOTIFICATION_TICKER));
      Preconditions.checkArgument(intent.hasExtra(EXTRA_NOTIFICATION_TITLE));
      Intent notificationIntent = new Intent(this,
        NodeMainExecutorService.class);
      notificationIntent.setAction(NodeMainExecutorService.ACTION_SHUTDOWN);
      PendingIntent pendingIntent = PendingIntent.getService(this, 0,
        notificationIntent, 0);
      notificationTitle = intent.getStringExtra(EXTRA_NOTIFICATION_TITLE);
      Notification notification = new Notification.Builder(this)
        .setSmallIcon(notificationSmallIcon)
        .setTicker(intent.getStringExtra(EXTRA_NOTIFICATION_TICKER))
        .setContentTitle(notificationTitle)
        .setContentText("Tap to shutdown.")
        .setContentIntent(pendingIntent)
        .build();
      startForeground(SHUTDOWN_NOTIFICATION, notification);
    }
    else if (intent.getAction().equals(ACTION_SHUTDOWN)) {
      shutdown();
    }
    
    return START_NOT_STICKY;
  }

  @Override
  public IBinder onBind(Intent intent) {
    return binder;
  }
  
  @Override
  public void onStart(ConnectedNode connectedNode) {
  }

  @Override
  public void onShutdown(Node node) {
  }
  
  @Override
  public void onShutdownComplete(Node node) {
  }

  @Override
  public void onError(Node node, Throwable throwable) {
  }

  public URI getMasterUri() {
    return masterUri;
  }

  public void setMasterUri(URI uri) {
    masterUri = uri;
  }

  public String getNamespace() {
    return namespace;
  }

  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }

  public void startMaster() {
    rosCore = RosCore.newPrivate();
    rosCore.start();
    try {
      rosCore.awaitStart();
    } catch (Exception e) {
      throw new RosRuntimeException(e);
    }
    masterUri = rosCore.getUri();
  }
}
