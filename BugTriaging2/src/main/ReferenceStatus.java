package main;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import utils.Constants.REFERENCE_TYPE;

public class ReferenceStatus {
	public HashMap<REFERENCE_TYPE, ArrayList<Date>> references;
	public int totalNumberOfReferencesUpToNow;
	public int successfulNumberOfReferencesUpToNow;
	public ReferenceStatus(){
		totalNumberOfReferencesUpToNow = 0;
		successfulNumberOfReferencesUpToNow = 0;
	}
}
