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

import android.content.ServiceConnection;
import android.content.ComponentName;
import android.os.IBinder;

import org.ros.android.NodeMainExecutorService;
import org.ros.android.NodeMainExecutorServiceListener;

/**
 * @author damonkohler@google.com (Damon Kohler)
 * @author ralf.kaestner@gmail.com (Ralf Kaestner)
 */
public class NodeMainExecutorServiceConnection implements ServiceConnection {
  private NodeMainExecutorService service = null;
  private NodeMainExecutorServiceListener listener = null;

  public NodeMainExecutorServiceConnection(NodeMainExecutorServiceListener
      listener) {
    this.listener = listener;
  }
  
  @Override
  public void onServiceConnected(ComponentName name, IBinder binder) {
    try {
      NodeMainExecutorService.Binder nodeMainExecutorServiceBinder =
        (NodeMainExecutorService.Binder)binder;
      service = nodeMainExecutorServiceBinder.getService();
    } catch (ClassCastException exception) {
      service = null;
      throw new ClassCastException(binder.toString()+
        " must be of type NodeMainExecutorService.Binder");
    }

    service.addListener(new NodeMainExecutorServiceListener() {
      @Override
      public void onConnect(NodeMainExecutorService nodeMainExecutorService) {
        if (listener != null)
          listener.onConnect(service);
      }
      
      @Override
      public void onDisconnect(NodeMainExecutorService
          nodeMainExecutorService) {
        if (listener != null)
          listener.onDisconnect(service);
      }

      @Override
      public void onShutdown(NodeMainExecutorService nodeMainExecutorService) {
        if (listener != null)
          listener.onShutdown(service);
      }
    });
  }

  @Override
  public void onServiceDisconnected(ComponentName name) {
  }
};
