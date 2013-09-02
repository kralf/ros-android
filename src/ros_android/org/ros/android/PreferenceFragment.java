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

import android.os.Bundle;
import android.util.Log;

/**
 * Allows the user to configue the master {@link URI} and common node
 * settings via a {@link PreferenceFragment}.
 * 
 * @author ralf.kaestner@gmail.com (Ralf Kaestner)
 */
public class PreferenceFragment
  extends android.preference.PreferenceFragment {
  
  /**
   * The key with which the last used {@link URI} will be stored as a
   * preference.
   */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    addPreferencesFromResource(org.ros.android.R.xml.ros_preferences);
  }
}
