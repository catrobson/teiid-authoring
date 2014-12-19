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
package org.teiid.authoring.client.widgets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.jboss.errai.ui.shared.api.annotations.DataField;
import org.jboss.errai.ui.shared.api.annotations.EventHandler;
import org.jboss.errai.ui.shared.api.annotations.Templated;
import org.teiid.authoring.client.dialogs.UiEvent;
import org.teiid.authoring.client.dialogs.UiEventType;
import org.teiid.authoring.client.messages.ClientMessages;
import org.teiid.authoring.client.resources.AppResource;
import org.teiid.authoring.client.services.NotificationService;
import org.teiid.authoring.client.services.QueryRpcService;
import org.teiid.authoring.client.services.TeiidRpcService;
import org.teiid.authoring.client.services.rpc.IRpcServiceInvocationHandler;
import org.teiid.authoring.client.utils.DdlHelper;
import org.teiid.authoring.share.Constants;
import org.teiid.authoring.share.beans.DataSourcePageRow;
import org.teiid.authoring.share.beans.QueryColumnBean;
import org.teiid.authoring.share.beans.QueryColumnResultSetBean;
import org.teiid.authoring.share.beans.QueryTableProcBean;
import org.teiid.authoring.share.services.StringUtils;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.ToggleButton;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;

@Templated("./JoinEditorPanel.html")
public class JoinEditorPanel extends Composite {

    @Inject
    private ClientMessages i18n;
    @Inject
    private NotificationService notificationService;
    
    @Inject
    protected TeiidRpcService teiidService;
    @Inject
    protected QueryRpcService queryService;
    
    @Inject Event<UiEvent> setDdlEvent;
    
    @Inject @DataField("lbl-joineditor-message")
    protected Label messageLabel;
    
    // Join Editor
    @Inject @DataField("listbox-sources")
    private ListBox listboxSources;
    @Inject @DataField("tbl-source-tables")
    private TablesProcNamesTable dsTablesTable;
    @Inject @DataField("tbl-lhs-columns")
    private ColumnNamesTable lhsJoinTable;
    @Inject @DataField("tbl-rhs-columns")
    private ColumnNamesTable rhsJoinTable;
    
    @Inject @DataField("btn-joineditor-setLHS")
    protected Button setLHSTableButton;
    @Inject @DataField("btn-joineditor-setRHS")
    protected Button setRHSTableButton;
    @Inject @DataField("btn-joineditor-setDdl")
    protected Button setDdlButton;
    
    @Inject @DataField("btn-joineditor-togglePanel")
    protected VerticalPanel togglePanel;
    
    protected ToggleButton joinInnerButton;
    protected ToggleButton joinLeftOuterButton;
    protected ToggleButton joinRightOuterButton;
    protected ToggleButton joinFullOuterButton;
    
    @Inject @DataField("listbox-lhcriteria")
    protected ListBox lhCriteriaListBox;
    @Inject @DataField("listbox-rhcriteria")
    protected ListBox rhCriteriaListBox;
    
	private Map<String,String> shortToLongTableNameMap = new HashMap<String,String>();
    private String lhTableName;
    private String rhTableName;
    private String lhTableSource;
    private String rhTableSource;
    private String joinType = Constants.JOIN_TYPE_INNER;
    private String currentStatus = "";
	private SingleSelectionModel<String> tableSelectionModel;
	private String selectedTable;
    
    /**
     * Called after construction.
     */
    @PostConstruct
    protected void postConstruct() {
    	setLHSTableButton.setEnabled(false);
    	setRHSTableButton.setEnabled(false);
    	
    	joinInnerButton = new ToggleButton(new Image(AppResource.INSTANCE.images().joinInner_Image()));
        joinLeftOuterButton = new ToggleButton(new Image(AppResource.INSTANCE.images().joinLeftOuter_Image()));
        joinRightOuterButton = new ToggleButton(new Image(AppResource.INSTANCE.images().joinRightOuter_Image()));
        joinFullOuterButton = new ToggleButton(new Image(AppResource.INSTANCE.images().joinFullOuter_Image()));
        joinInnerButton.setTitle("Inner Join");
        joinLeftOuterButton.setTitle("Left Outer Join");
        joinRightOuterButton.setTitle("Right Outer Join");
        joinFullOuterButton.setTitle("Full Outer Join");
    	DOM.setStyleAttribute(joinLeftOuterButton.getElement(), "margin-top", "5px");
    	DOM.setStyleAttribute(joinRightOuterButton.getElement(), "margin-top", "5px");
    	DOM.setStyleAttribute(joinFullOuterButton.getElement(), "margin-top", "5px");

        togglePanel.add(joinInnerButton);
        togglePanel.add(joinLeftOuterButton);
        togglePanel.add(joinRightOuterButton);
        togglePanel.add(joinFullOuterButton);
        
        // Default to inner join
        joinInnerButton.setValue(true);
        joinLeftOuterButton.setValue(false);
        joinRightOuterButton.setValue(false);
        joinFullOuterButton.setValue(false);
        joinType=Constants.JOIN_TYPE_INNER;
        
        joinInnerButton.addClickHandler(new ClickHandler() {
    		public void onClick(ClickEvent event) {
    			joinType=Constants.JOIN_TYPE_INNER;
    			joinInnerButton.setValue(true);
    			joinLeftOuterButton.setValue(false);
    			joinRightOuterButton.setValue(false);
    			joinFullOuterButton.setValue(false);
    		}
    	});                	
        joinLeftOuterButton.addClickHandler(new ClickHandler() {
    		public void onClick(ClickEvent event) {
    			joinType=Constants.JOIN_TYPE_LEFT_OUTER;
    			joinInnerButton.setValue(false);
    			joinLeftOuterButton.setValue(true);
    			joinRightOuterButton.setValue(false);
    			joinFullOuterButton.setValue(false);
    		}
    	});                	
        joinRightOuterButton.addClickHandler(new ClickHandler() {
    		public void onClick(ClickEvent event) {
    			joinType=Constants.JOIN_TYPE_RIGHT_OUTER;
    			joinInnerButton.setValue(false);
    			joinLeftOuterButton.setValue(false);
    			joinRightOuterButton.setValue(true);
    			joinFullOuterButton.setValue(false);
    		}
    	});                	
        joinFullOuterButton.addClickHandler(new ClickHandler() {
    		public void onClick(ClickEvent event) {
    			joinType=Constants.JOIN_TYPE_FULL_OUTER;
    			joinInnerButton.setValue(false);
    			joinLeftOuterButton.setValue(false);
    			joinRightOuterButton.setValue(false);
    			joinFullOuterButton.setValue(true);
    		}
    	});  
        
        // Change Listener for LHS criteria ListBox
        listboxSources.addChangeHandler(new ChangeHandler()
        {
        	// Changing the updates status
        	public void onChange(ChangeEvent event)
        	{
        		dsTablesTable.clear();
    			String srcName = getSelectedSource();
    			if (srcName != null) {
    				doGetTablesAndProcs(srcName);
    			}
    			setLHSTableButton.setEnabled(false);
    			setRHSTableButton.setEnabled(false);
        	}
        });
        // Change Listener for LHS criteria ListBox
        lhCriteriaListBox.addChangeHandler(new ChangeHandler()
        {
        	// Changing the updates status
        	public void onChange(ChangeEvent event)
        	{
                updateStatus();
        	}
        });
        // Change Listener for RHS criteria ListBox
        rhCriteriaListBox.addChangeHandler(new ChangeHandler()
        {
        	// Changing the updates status
        	public void onChange(ChangeEvent event)
        	{
                updateStatus();
        	}
        });
        
    	// SelectionModel to handle Table-procedure selection 
    	tableSelectionModel = new SingleSelectionModel<String>();
    	dsTablesTable.setSelectionModel(tableSelectionModel); 
    	tableSelectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
    		public void onSelectionChange( SelectionChangeEvent event) { 
    			String selected = tableSelectionModel.getSelectedObject();
    			selectedTable = selected;
    			if (selected != null) {
    				setLHSTableButton.setEnabled(true);
    				setRHSTableButton.setEnabled(true);
    			} else {
    				setLHSTableButton.setEnabled(false);
    				setRHSTableButton.setEnabled(false);
    			}
    		} });
    	
        doGetQueryableSources();
    }
    
    /**
     * Get the selected DataSource
     * @return
     */
    public String getSelectedSource() {
    	int index = listboxSources.getSelectedIndex();
    	return listboxSources.getValue(index);
    }
    
    protected void doGetQueryableSources( ) {
    	teiidService.getDataSources("filter", Constants.SERVICE_SOURCE_VDB_PREFIX, new IRpcServiceInvocationHandler<List<DataSourcePageRow>>() {
    		@Override
    		public void onReturn(List<DataSourcePageRow> dsInfos) {
    			// Create list of DataSources that are accessible.  Only the Sources that have 'OK' state
    			// have an associated VDB source and are reachable...
            	List<String> dsList = new ArrayList<String>();
    			for(DataSourcePageRow row : dsInfos) {
    				if(row.getState()==DataSourcePageRow.State.OK) {
            			dsList.add(row.getName());
    				}
    			}
    			populateSourcesListBox(dsList);
            	updateStatus();
    		}
    		@Override
    		public void onError(Throwable error) {
                notificationService.sendErrorNotification(i18n.format("vieweditor-panel.error-getting-svcsources"), error); //$NON-NLS-1$
    		}
    	});
    }
    
    /**
     * Get the Tables and Procs for the supplied data source
     * @param dataSourceName the name of the source
     */
    protected void doGetTablesAndProcs(String dataSourceName) {
    	String vdbSrcName = Constants.SERVICE_SOURCE_VDB_PREFIX+dataSourceName;
    	String vdbSrcJndi = Constants.JNDI_PREFIX+vdbSrcName;
		queryService.getTablesAndProcedures(vdbSrcJndi, vdbSrcName, new IRpcServiceInvocationHandler<List<QueryTableProcBean>>() {
			@Override
			public void onReturn(List<QueryTableProcBean> tablesAndProcs) {
				List<String> nameList = new ArrayList<String>();
				shortToLongTableNameMap.clear();
				for(QueryTableProcBean tp : tablesAndProcs) {
					String name = tp.getName();
					if(name!=null) {
						if(name.contains(".PUBLIC.")) {
							String shortName = name.substring(name.indexOf(".PUBLIC.")+".PUBLIC.".length());
							shortToLongTableNameMap.put(shortName, name);
							nameList.add(shortName);
						} else if(!name.contains(".INFORMATION_SCHEMA.")) {
							shortToLongTableNameMap.put(name, name);
							nameList.add(name);
						}
					}
				}
				//tableSelectionModel.clear();
				dsTablesTable.setData(nameList);
            	updateStatus();
			}
			@Override
			public void onError(Throwable error) {
				notificationService.sendErrorNotification(i18n.format("vieweditor-panel.error-getting-tables-procs"), error); //$NON-NLS-1$
			}
		});

    }
    
    private void populateSourcesListBox(List<String> dsNames) {
    	// Make sure clear first
    	listboxSources.clear();

    	listboxSources.insertItem(Constants.NO_DATASOURCE_SELECTION, 0);
    	
    	// Repopulate the ListBox with column names
    	int i = 1;
    	for(String dsName: dsNames) {
    		listboxSources.insertItem(dsName, i);
    		i++;
    	}

    	// Initialize by setting the selection to the first item.
    	listboxSources.setSelectedIndex(0);
    }
    
    public String getLHTable() {
    	return this.lhTableName;
    }
    public String getRHTable() {
    	return this.rhTableName;
    }
    public String getLHTableSource() {
    	return this.lhTableSource;
    }
    public String getRHTableSource() {
    	return this.rhTableSource;
    }
    
    /**
     * Event handler that fires when the user clicks the Add to view defn button.
     * @param event
     */
    @EventHandler("btn-joineditor-setLHS")
    public void onSetLHSButtonClick(ClickEvent event) {
    	// Get selected source / table and populate
    	String srcName   = getSelectedSource();
    	lhTableName = selectedTable;
    	lhTableSource = srcName;
    	String longTableName = shortToLongTableNameMap.get(lhTableName);
    	
    	populateLHSTable(srcName,longTableName);
    }

    /**
     * Event handler that fires when the user clicks the Add to view defn button.
     * @param event
     */
    @EventHandler("btn-joineditor-setRHS")
    public void onSetRHSButtonClick(ClickEvent event) {
    	// Get selected source / table and populate
    	String srcName   = getSelectedSource();
    	rhTableName = selectedTable;
    	rhTableSource = srcName;
    	String longTableName = shortToLongTableNameMap.get(rhTableName);
    	
    	populateRHSTable(srcName,longTableName);
    }
    
    private void populateLHCriteriaListBox(List<String> columnNames) {
    	// Make sure clear first
    	lhCriteriaListBox.clear();

    	lhCriteriaListBox.insertItem(Constants.NO_CRITERIA_SELECTION, 0);
    	
    	// Repopulate the ListBox with column names
    	int i = 1;
    	for(String columnName: columnNames) {
    		lhCriteriaListBox.insertItem(columnName, i);
    		i++;
    	}

    	// Initialize by setting the selection to the first item.
    	lhCriteriaListBox.setSelectedIndex(0);
    }
    
    /**
     * Get the selected LH Criteria Column
     * @return
     */
    public String getLHCriteriaSelection() {
    	int index = lhCriteriaListBox.getSelectedIndex();
    	return lhCriteriaListBox.getValue(index);
    }
    
    /**
     * Set the selected LH Criteria Column
     * @return
     */
	public void setLHCriteriaSelection(String columnName) {
		int indx = 0;
		int nItems = lhCriteriaListBox.getItemCount();
		for(int i=0; i<nItems; i++) {
			String itemText = lhCriteriaListBox.getItemText(i);
			if(itemText.equalsIgnoreCase(columnName)) {
				indx = i;
				break;
			}
		}
		lhCriteriaListBox.setSelectedIndex(indx);
	}
	
    private void populateRHCriteriaListBox(List<String> columnNames) {
    	// Make sure clear first
    	rhCriteriaListBox.clear();

    	rhCriteriaListBox.insertItem(Constants.NO_CRITERIA_SELECTION, 0);
    	
    	// Repopulate the ListBox with column names
    	int i = 1;
    	for(String columnName: columnNames) {
    		rhCriteriaListBox.insertItem(columnName, i);
    		i++;
    	}

    	// Initialize by setting the selection to the first item.
    	rhCriteriaListBox.setSelectedIndex(0);
    }
    
    /**
     * Get the selected RH Criteria Column
     * @return
     */
    public String getRHCriteriaSelection() {
    	int index = rhCriteriaListBox.getSelectedIndex();
    	return rhCriteriaListBox.getValue(index);
    }
    
    /**
     * Set the selected RH Criteria Column
     * @return
     */
	public void setRHCriteriaSelection(String columnName) {
		int indx = 0;
		int nItems = rhCriteriaListBox.getItemCount();
		for(int i=0; i<nItems; i++) {
			String itemText = rhCriteriaListBox.getItemText(i);
			if(itemText.equalsIgnoreCase(columnName)) {
				indx = i;
				break;
			}
		}
		rhCriteriaListBox.setSelectedIndex(indx);
	}

	/**
     * Event handler that fires when the user clicks the Add to view defn button.
     * @param event
     */
    @EventHandler("btn-joineditor-setDdl")
    public void onSetDdlButtonClick(ClickEvent event) {
    	String ddl = formDdl();
    	
		UiEvent uiEvent = new UiEvent(UiEventType.VIEW_DEFN_REPLACE_FROM_JOIN_EDITOR);
		uiEvent.setViewDdl(ddl);
		List<String> viewSources = new ArrayList<String>();
		viewSources.add(getLHTableSource());
		viewSources.add(getRHTableSource());
		uiEvent.setViewSources(viewSources);
		
		setDdlEvent.fire(uiEvent);
    }
    
    private String formDdl( ) {
    	List<String> lhsColNames = lhsJoinTable.getSelectedColumnNames();
    	List<String> lhsColTypes = lhsJoinTable.getSelectedColumnTypes();
    	List<String> rhsColNames = rhsJoinTable.getSelectedColumnNames();
    	List<String> rhsColTypes = rhsJoinTable.getSelectedColumnTypes();
    	String lhsCriteriaCol = getLHCriteriaSelection();
    	String rhsCriteriaCol = getRHCriteriaSelection();
    	String lhsTableName = getLHTable();
    	String rhsTableName = getRHTable();
    	String jType = this.joinType;
    	
     	String viewDdl = DdlHelper.getODataViewJoinDdl(Constants.SERVICE_VIEW_NAME, lhsTableName, lhsColNames, lhsColTypes, lhsCriteriaCol,
     			                                                                    rhsTableName, rhsColNames, rhsColTypes, rhsCriteriaCol, jType);
    	
     	return viewDdl;
    }
    
    /**
     * Populate LHS Table
     */
    protected void populateLHSTable(String source, String table) {
    	String filterText = "";
    	String vdbSrcJndi = Constants.JNDI_PREFIX+Constants.SERVICE_SOURCE_VDB_PREFIX+source;

    	queryService.getQueryColumnResultSet(1, filterText, vdbSrcJndi, table,
    			new IRpcServiceInvocationHandler<QueryColumnResultSetBean>() {
    		@Override
    		public void onReturn(QueryColumnResultSetBean data) {
    			List<CheckableNameTypeRow> colList = new ArrayList<CheckableNameTypeRow>();
    			List<String> colNames = new ArrayList<String>();
    			List<QueryColumnBean> qColumns = data.getQueryColumns();
    			for(QueryColumnBean col : qColumns) {
    				CheckableNameTypeRow cRow = new CheckableNameTypeRow();
    				cRow.setName(col.getName());
    				cRow.setType(col.getType());
    				colList.add(cRow);
    				colNames.add(col.getName());
    			}
    			lhsJoinTable.setData(colList);
    			populateLHCriteriaListBox(colNames);
    			updateStatus();
    		}
    		@Override
    		public void onError(Throwable error) {
//    			notificationService.sendErrorNotification(i18n.format("vieweditor-panel.error-getting-tablecols"), error); //$NON-NLS-1$
    		}
    	});

    }
    
    /**
     * Populate RHS Table
     */
    protected void populateRHSTable(String source, String table) {
    	String filterText = "";
    	String vdbSrcJndi = Constants.JNDI_PREFIX+Constants.SERVICE_SOURCE_VDB_PREFIX+source;

    	queryService.getQueryColumnResultSet(1, filterText, vdbSrcJndi, table,
    			new IRpcServiceInvocationHandler<QueryColumnResultSetBean>() {
    		@Override
    		public void onReturn(QueryColumnResultSetBean data) {
    			List<CheckableNameTypeRow> colList = new ArrayList<CheckableNameTypeRow>();
    			List<String> colNames = new ArrayList<String>();
    			List<QueryColumnBean> qColumns = data.getQueryColumns();
    			for(QueryColumnBean col : qColumns) {
    				CheckableNameTypeRow cRow = new CheckableNameTypeRow();
    				cRow.setName(col.getName());
    				cRow.setType(col.getType());
    				colList.add(cRow);
    				colNames.add(col.getName());
    			}
    			rhsJoinTable.setData(colList);
    			populateRHCriteriaListBox(colNames);
    			updateStatus();
    		}
    		@Override
    		public void onError(Throwable error) {
//    			notificationService.sendErrorNotification(i18n.format("vieweditor-panel.error-getting-tablecols"), error); //$NON-NLS-1$
    		}
    	});

    }
    
    /**
     * Handles UiEvents from columnNamesTable
     * @param dEvent
     */
    public void onUiEvent(@Observes UiEvent dEvent) {
    	// checkbox change event from column names table
    	if(dEvent.getType() == UiEventType.COLUMN_NAME_TABLE_CHECKBOX_CHANGED) {
    		updateStatus();
    	}
    }
    
    /**
     * Update panel status
     */
	private void updateStatus( ) {
    	currentStatus = Constants.OK;
    	
    	// Ensure LH and RH tables are selected
    	if(StringUtils.isEmpty(getLHTable()) || StringUtils.isEmpty(getRHTable())) {
    		currentStatus = "Define the Left and Right tables by choosing from available tables, then click LHS > or RHS >";
    	}
    	
		// Ensure some columns are selected
    	if(Constants.OK.equals(currentStatus)) {
    		List<String> selectedLHColumns = lhsJoinTable.getSelectedColumnNames();
    		List<String> selectedRHColumns = rhsJoinTable.getSelectedColumnNames();
    		if(selectedLHColumns.isEmpty() && selectedRHColumns.isEmpty()) {
    			currentStatus = "Check one or more columns for the SELECT";
    		}
    	}
    	
		// Make sure LH Criteria is selected
    	if(Constants.OK.equals(currentStatus)) {
    		String lhCritColumn = getLHCriteriaSelection();
    		if(Constants.NO_CRITERIA_SELECTION.equals(lhCritColumn)) {
    			currentStatus = "Select the LHS join criteria column";
    		}
    	}

    	// Make sure RH Criteria is selected
    	if(Constants.OK.equals(currentStatus)) {
    		String rhCritColumn = getRHCriteriaSelection();
    		if(Constants.NO_CRITERIA_SELECTION.equals(rhCritColumn)) {
    			currentStatus = "Select the RHS join criteria column";
    		}
    	}
    	
		// Enable setDdlButton button if OK
    	if(Constants.OK.equals(currentStatus)) {
    		messageLabel.setText("Click the 'Apply' button when finished with service definition");
    		setDdlButton.setEnabled(true);
    	} else {
    		messageLabel.setText(currentStatus);
    		setDdlButton.setEnabled(false);
    	}

    }
	
	public String getStatus() {
		return this.currentStatus;
	}
           
}