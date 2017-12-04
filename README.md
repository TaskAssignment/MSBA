# MSBA (Multi-source Bug assignment)

This project includes the experiments for multi-source bug assignment (using Github data sets).

The bug assignment is based on expertise scores of the developers. The scores are obtained by calculating the similarity between the new bug and each developer. Our similarity metric is based on IR method, TF-IDF, and takes into account time and importance of the used keywords as well as the context of the usage (e.g., the text of a commit, a pull request comment or a previously fixed bug). 

All the MSBA experiment requirements (data set, code, etc.) are available here.
The data set for 13 popular Github projects including:
- 7,438 developers
- 64,474 bug reports
- 93,016 bug-assignments
- 281,336 commits
- 80,803 pull requests
- 351,940 bug comments
- 30,155 commit comments
- 298,579 pull request comments

In addition, there is another data set for multi-project experiment (using evidence of expertise from sub-projects to assign bugs in the parent project) including two popular projects (angular/angular.js and rails/rails) and their 6 and 13 sub-projects:
- 2,574 developers (as sub-project members)
- 11,952 bug reports
- 17,426 bug-assignments
- 17,352 commits
- 5,740 pull requests
- 50,437comments
- 1,712 commit comments
- 16,414 pull request comments


Here's the data model of our experiments:
![Alt text](/Schema.png?raw=true "The data model")


How to run the code:

1- Clone the code in the root of your drive "C". It should create the folder "C:\\MSBA" and all its contents. After cloning, in this directory ("C:\\MSBA") there should be "BugTriaging2" and "Exp" folders. The first one includes the source codes. The second folder includes the requirements of the experiments (all the input files and the output files that are results of runnning the code for different configurations). These are the results we provided in our paper.

2- Install Eclipse IDE for Java Developers. The program is tested on Eclipse Neon.3 Release 4.6.3: http://www.eclipse.org/downloads/packages/eclipse-ide-java-developers/neon3 on a 64 bit version with MS Windows 10 OS. However, other releases and even other versions of Eclipse and on different platforms should be fine.

3- You need to install two .jar files; "json-simple-1.1.1.jar" and "guava-10.0.1.jar" that are provided in the jar folder. For doing this, you need to right click on "src" in your project in Eclipse, then choose "Build Path" --> "Configure build path". Then go to "Libraries" tab and select "Add external jars". Then brows to the folder containing those two .jar files and select them and add them to your library.

4- If you are running the code on Linux, you need to change the formatting of the paths in the code (in Constants.java, CSVManipulations.java, CheckFeasibility.java and JSONToTSV.java); change "\\\\" to "//" everywhere.

5- The main file to run is "BugTriaging2C\src\main\Algorithm.java". Note that there are several loops in the main() method of this class. These were used once for tuning and running the "bugAssignment()" method under different configurations. But all of the extra cases are now disabled and the "bugAssignment()" method is executed just once every time we run the code. After running, the output should be created if the "Out" folder is empty. Otherwise it will add the new results at the end of the contents of the output files. Currently the program is set to  run just T5 (combination of all assignee types). But if you want to run for T1, T2, T3 and T4 assignee types separately you can change "0"s to "1"s in "assignmentTypesToTriage" array in main(). 
The "runType = RUN_TYPE.RUN_FOR_ALL_PROJECTS" statement makes the experiment() method run the code for all the projects. If you want to run the code for specific projects (like angular and rails and ther sub-projects, or just three test projects), then you can comment this line and un-comment one of the following lines. 
"runType_SO" contains the status of Stack Overflow data. Currently it is set to "DO_NOT_CONSIDER_SO", but you can comment that line and un-comment one of the following lines.
"option13_counter" is set to 11 which represents "1011" in binary (COMMIT_COMMENT=1, COMMIT=0, BUG_COMMENT=1. BUG=1). It can be changed to a value from 0 to 15 (representing "0000" to "1111" in binary).


6- If you have any problems or issues, you can create an issue in this repository and I will answer to that. Or you can contact "**alisajedi [at] ualberta.ca**" for other questions.

Thank you for your interest in our project. Ali Sajedi.

