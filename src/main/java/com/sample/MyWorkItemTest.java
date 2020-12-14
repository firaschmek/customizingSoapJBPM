package com.sample;

import java.util.HashMap;
import java.util.Map;

import org.drools.core.process.instance.WorkItemHandler;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemManager;

public class MyWorkItemTest implements WorkItemHandler{

	@Override
	public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
		 workItem.getParameters().toString();

	        /**Input Variables***/
	        String stringVar = (String) workItem.getParameter("stringVar");

	        System.out.println("Started : "+stringVar);
	        /***
	         * 
	         * 
	         * YOUR CODE
	         * 
	         */

	        String msg = "done";

	        /**Output Variables in a HashMap***/
	        Map<String, Object> resultMap = new HashMap<String, Object>();
	        resultMap.put("Result", msg); //("name of variable", value)
	        manager.completeWorkItem(workItem.getId(), resultMap);
		
	}

	@Override
	public void executeWorkItem(WorkItem arg0, WorkItemManager arg1) {
		  System.out.println("Aborted ! ");
		
	}

}
