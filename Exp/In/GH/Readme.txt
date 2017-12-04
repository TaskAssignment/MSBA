There are two backup folders in this data set:
1) Data for Experiment 1 (MultiSource - only data from inside 13 projects)
2) Data for Experiment 2 (MultiSource - data from inside 2 projdets, their subProjects and SO)

In order to run the code for multi-source experiment inside 13 projects, use the first one (already copied in the "Exp\in\GH" folder). 

Otherwise, if you want to run the code for more complicated multi-source experiment (using evidence from inside the two projects angular.js and rails, and their respectively 6 and 13 sub-projects, as well as Stack Overflow answers), use the second one. In that case, you need to remove all the current files of "Exp\in\GH" folder and copy the files of the second folder there. Also you need to change the settings of the program (in the method "experiment" of the code file "Algorithm.java") to consider those extra evidence as well.  