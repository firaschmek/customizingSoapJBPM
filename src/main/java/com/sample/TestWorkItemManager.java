package com.sample;

import java.util.Map;

import org.drools.core.process.instance.impl.WorkItemImpl;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.process.WorkItemManager;

public class TestWorkItemManager implements WorkItemManager{

	   private WorkItem workItem;

       

       TestWorkItemManager(WorkItem workItem) {

           this.workItem = workItem;

       }



       @Override

       public void completeWorkItem(long id, Map<String, Object> results) {

           ((WorkItemImpl)workItem).setResults(results);

           

       }



       @Override

       public void abortWorkItem(long id) {

           

       }



       @Override

       public void registerWorkItemHandler(String workItemName, WorkItemHandler handler) {

           

       }



	
}
