import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import com.taskadapter.redmineapi.IssueManager;
import com.taskadapter.redmineapi.RedmineException;
import com.taskadapter.redmineapi.RedmineManager;
import com.taskadapter.redmineapi.RedmineManagerFactory;
import com.taskadapter.redmineapi.bean.Issue;
import com.taskadapter.redmineapi.bean.IssueFactory;
import com.taskadapter.redmineapi.bean.Project;
import com.taskadapter.redmineapi.bean.User;

public class RedmineUpdate
{
	private String uri;
	private String apiAccessKey;
	private String projectName;
	private String name;
	private Integer queryId = null;
	private List<Issue> issues;
	private RedmineManager mgr;
	private IssueManager issueManager;
	
	public RedmineUpdate(String uri, String apiAccessKey, String projectName,
			String name) {
		this.uri = uri;
		this.apiAccessKey = apiAccessKey;
		this.projectName = projectName;
		this.name = name;
		mgr = RedmineManagerFactory.createWithApiKey(uri, apiAccessKey);
	}
	
	public void updateIssuesUsingFile(String fileName) {
		Integer projectId;
		BufferedReader br;
		String line;
		
		/* get the project id */
		try {
			projectId = getProjectId(mgr, projectName);
			if (projectId == null) {
				System.out.println("Project not found. Abort!");
				return;
			}
		} catch (RedmineException e) {
			System.out.println("Error getting project id. Abort!");
			e.printStackTrace();
			return;
		}
		
		/* get the issues for the project */
		try {
			issueManager = mgr.getIssueManager();
			issues = issueManager.getIssues(projectId.toString(), queryId);
		} catch (RedmineException e) {
			System.out.println("Error getting issues. Abort!");
			e.printStackTrace();
		}
		
		/* navigate through results file and update the issues from Redmine */
		try {
			br = new BufferedReader(new FileReader(fileName));
			while ((line = br.readLine()) != null) {
				String[] arr = line.split(" ");
				System.out.println(line);
				if (arr.length != 3) {
					System.out.println("Format error for \"results\" file");
				}
				
				Issue issue = getIssueWithSubject(arr[0]);
				if (issue == null) {
					System.out.println("Create new issue on Redmine: " + arr[0]);
					Issue newIssue = IssueFactory.create(projectId, arr[0]);
					newIssue.setSubject(arr[0]);
					newIssue.setDescription(arr[1]);
					if (Integer.valueOf(arr[2]) == 0)
						newIssue.setStatusId(3);
					else 
						newIssue.setStatusId(2);
					User user = getUserByName(name);
					if (user != null)
						newIssue.setAssignee(user);
					issueManager.createIssue(newIssue);
				
				}
				else {
					System.out.println("Updating issue on Redmine: " + arr[0]);
					issue.setStatusId(8);
					User user = getUserByName("doru");
					if (user != null)
						issue.setAssignee(user);
					issueManager.update(issue);
				}
			}
		} catch (FileNotFoundException e) {
			System.out.println("File results not found");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("Error reading from file");
			e.printStackTrace();
		} catch (RedmineException e) {
			System.out.println("Error creating issue.");
			e.printStackTrace();
		}	
	}
	
    private Issue getIssueWithSubject(String subject) {
    	for (Issue issue : issues) {
    		if (issue.getSubject().equalsIgnoreCase(subject))
    			return issue;
        }
    	return null;
    }
    
    private Integer getProjectId(RedmineManager mgr, String projectName) throws RedmineException {
    	List<Project> projects = mgr.getProjectManager().getProjects();
    	for (Project project : projects) {
    		if (project.getName().equals(projectName))
    			return project.getId();
    	}
    	return null;
    }
    
    private User getUserByName(String userName) {
    	List<User> users = null;
		try {
			users = mgr.getUserManager().getUsers();
		} catch (RedmineException e) {
			System.out.println("Error getting user list.");
			e.printStackTrace();
		}
    	for (User user : users) {
    		if (user.getFirstName().equalsIgnoreCase(userName))
    			return user;
    	}
    	return null;
    }
    
    public static void main(String[] args) {
    	RedmineUpdate redmine = new RedmineUpdate("redmine address", 
    							"api access key", "MPTCP", "doru");
    	redmine.updateIssuesUsingFile("results");
    }
}
