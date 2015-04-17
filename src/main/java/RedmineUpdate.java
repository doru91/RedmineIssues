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
		String line, description = "";
		
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
				if (arr.length != 3) {
					System.out.println("Format error for \"results\" file");
				}
				
				description = "";
				if (arr[2].endsWith("!") == false) {
					while ((line = br.readLine()) != null) {
						description = description.concat("\n").concat(line);
						if (line.endsWith("!"))
							break;
					}
				}
				
				/* check if an issue with this name already exists */
				Issue issue = getIssueWithSubject(arr[0]);
				
				/* create a new issue */
				if (issue == null) {
					System.out.println("Create new issue on Redmine: " + arr[0]);
					Issue newIssue = IssueFactory.create(projectId, arr[0]);
					newIssue.setSubject(arr[0]);
					setIssueProperties (newIssue, description, arr[2], arr[1], name);
					issueManager.createIssue(newIssue);
				}
				/* or update an existing one */
				else {
					System.out.println("Updating issue on Redmine: " + arr[0]);
					setIssueProperties (issue, description, arr[2], arr[1], name);
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
	
	private void setIssueProperties(Issue issue, String description, String status, String path, String userName) {
		if (status.endsWith("!"))
			status = status.substring(0, status.length() -1);
		
		if (Integer.valueOf(status) == 0)
			issue.setStatusId(3);
		else
			issue.setStatusId(2);
		User user = getUserByName(userName);
		if (user != null)
			issue.setAssignee(user);
		
		if (issue.getDescription().length() > 0 && description.length() == 0)
			return;
		
		description = description.concat("\n \n Path for test: " + path);
		issue.setDescription(description);
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
    	String userName = "doru";
    	
    	if (args.length > 0)
    			userName = args[0];
    	
    	RedmineUpdate redmine = new RedmineUpdate("http://eureka.rb.intel.com/redmine", 
    							"9a76ae54ea219284761b166894dc7cdc0b30a733", "MPTCP", userName);
    	redmine.updateIssuesUsingFile("results");
    }
}
