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

import android.app.DialogFragment;
import android.app.Dialog;
import android.view.View;
import android.view.LayoutInflater;
import android.content.DialogInterface;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;
import android.util.Log;
import android.util.AndroidRuntimeException;

import org.ros.node.NodeConfiguration;

import java.lang.Integer;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.ros.android.MasterChooserDialogFragmentListener;

/**
 * Allows the user to configue the master {@link URI} via a
 * {@link DialogFragment}.
 * 
 * @author ralf.kaestner@gmail.com (Ralf Kaestner)
 */
public class MasterChooserDialogFragment extends DialogFragment {
  /**
   * The key with which the last used {@link URI} will be stored as a
   * preference.
   */
  private static final String PREFS_KEY_MASTER_URI = "pref_key_ros_master_uri";
  private static final String PREFS_KEY_NAMESPACE = "pref_key_namespace";

  private URI masterUri = NodeConfiguration.DEFAULT_MASTER_URI;
  private String namespace;
  
  private View view;
  private EditText hostEditText;
  private EditText portEditText;
  
  private MasterChooserDialogFragmentListener listener;
  
  public URI getMasterUri() {
    return masterUri;
  }
  
  public String getNamespace() {
    return namespace;
  }
  
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    LayoutInflater inflater = getActivity().getLayoutInflater();
    view = inflater.inflate(R.layout.master_chooser_dialog_fragment, null);
    hostEditText = (EditText)view.findViewById(R.id.master_chooser_host);
    portEditText = (EditText)view.findViewById(R.id.master_chooser_port);
    
    SharedPreferences preferences = 
      PreferenceManager.getDefaultSharedPreferences(getActivity());
    String masterUriString = preferences.getString(PREFS_KEY_MASTER_URI,
      NodeConfiguration.DEFAULT_MASTER_URI.toString());
    namespace = preferences.getString(PREFS_KEY_NAMESPACE, "");
  
    try {
      URI masterUri = new URI(masterUriString);
      hostEditText.setText(masterUri.getHost());
      portEditText.setText(Integer.toString(masterUri.getPort()));
    } catch (URISyntaxException exception) {
      throw new AndroidRuntimeException(
        String.format("Syntax error in URI: %s.", masterUriString));
    }
    hostEditText.requestFocus();
    
    builder.setView(view);
    builder.setPositiveButton(R.string.master_connect,
        new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int id) {
        onConnectClick();
      }
    });
    builder.setNegativeButton(android.R.string.cancel,
        new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int id) {
        onCancelClick();
      }
    });

    try {
      listener = (MasterChooserDialogFragmentListener)getActivity();
    } catch (ClassCastException exception) {
      listener = null;
    }
    
    return builder.create();
  }

  public void onConnectClick() {
    EditText hostEditText = (EditText)view.findViewById(
      R.id.master_chooser_host);
    EditText portEditText = (EditText)view.findViewById(
      R.id.master_chooser_port);
      
    String masterHost = hostEditText.getText().toString();
    String masterPort = portEditText.getText().toString();

    if (masterHost.isEmpty()) {
      masterHost = NodeConfiguration.DEFAULT_MASTER_URI.getHost();
      hostEditText.setText(masterHost);
    }
    if (masterPort.isEmpty()) {
      masterPort = Integer.toString(
        NodeConfiguration.DEFAULT_MASTER_URI.getPort());
      portEditText.setText(masterPort);
    }    
    
    String masterUriString = String.format("http://%s:%s", masterHost,
      masterPort);
    try {
      URI masterUri = new URI(masterUriString);
      MasterChooserDialogFragment.this.masterUri = masterUri;
    } catch (URISyntaxException exception) {
      Toast.makeText(getActivity(), String.format("Invalid URI: %s",
        masterUriString), Toast.LENGTH_SHORT).show();
      MasterChooserDialogFragment.this.getDialog().cancel();
      
      return;
    }
    
    SharedPreferences preferences = 
      PreferenceManager.getDefaultSharedPreferences(getActivity());
    SharedPreferences.Editor editor = preferences.edit();
    editor.putString(PREFS_KEY_MASTER_URI, masterUri.toString());
    editor.commit();

    if (listener != null)
      listener.onConnectClick(this);
  }

  public void onCancelClick() {
    if (listener != null)
      listener.onCancelClick(this);
      
    MasterChooserDialogFragment.this.getDialog().cancel();
  }
}
