/*
 * Copyright 2014 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.teiid.authoring.client.dialogs;

import org.kie.uberfire.client.common.Popup;

import com.google.gwt.user.client.ui.Widget;

/**
 * Popup which shows a title and status message
 */
public class StatusPopup extends Popup {

	private StatusPopupPanel contentPanel;

    public StatusPopup(final StatusPopupPanel contentPanel, final String title) {
        setup( contentPanel, title);
    }

    protected void setup(final StatusPopupPanel contentPanel, final String title) {
    	this.contentPanel = contentPanel;
    	
        setModal( true );
        
        setTitle(title);
    }

    @Override
    public Widget getContent() {
        return this.contentPanel;
    }
    
    public void setContentTitle(String title) {
    	this.contentPanel.setTitle(title);
    }

    public void setContentMessage(String message) {
    	this.contentPanel.setMessage(message);
    }
    
    public void setSize(int width, int height) {
    	//this.contentPanel.setSize(width+"px", height+"px");
    }
}
