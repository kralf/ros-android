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
import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.EditText;
import android.util.Log;

import org.ros.node.NodeConfiguration;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * Allows the user to configue the master {@link URI} via a
 * {@link DialogFragment}.
 * 
 * @author ralf.kaestner@gmail.com (Ralf Kaestner)
 */
public class MasterChooserDialogFragment extends DialogFragment {
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    LayoutInflater inflater = getActivity().getLayoutInflater();
    View view = inflater.inflate(
      R.layout.master_chooser_dialog_fragment, null);
    EditText hostEditText = (EditText)view.findViewById(
      R.id.master_chooser_host);
    hostEditText.requestFocus();
    
    builder.setView(view);
    builder.setPositiveButton(R.string.master_connect,
        new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int id) {
      }
    });
    builder.setNegativeButton(android.R.string.cancel,
        new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int id) {
        MasterChooserDialogFragment.this.getDialog().cancel();
      }
    });

    return builder.create();
  }
}
