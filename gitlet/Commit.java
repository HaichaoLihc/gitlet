package gitlet;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

import static gitlet.HelperMethods.*;

/** Represents a gitlet commit object.
 *  each instance has reference to parent and child commit id name,
 *  Commit 0's parent is set to null.
 *  and it contains a tree map of the file names and ids being tracked.
 *  each instance is serialized in commit file with id as file name.
 *  @author Haichao
 */
public class Commit implements Serializable {

    /**
     * The message of this Commit.
     */
    private String message;

    /**
     * the date of the commit
     */
    private Date timeStamp;

    /**
     * the parent commit
     */
    private String parentId;

    /**
     * a map of all tracked files, with id : name
     */
    private Map<String, String> trackedFiles = new TreeMap<>();

    /**
     * the unique id of the commit
     */
    private String id;

    private String secondParentId;

    public Commit(Date time, String msg) {
        timeStamp = time;
        message = msg;
        id = Utils.sha1((Object) Utils.serialize(this));
        parentId = null;
    }

    public Commit(String msg, String parentId) {
        timeStamp = new Date();
        message = msg;
        id = Utils.sha1((Object) Utils.serialize(this));
        this.parentId = parentId;
        // get parent commit object and inherit tracked files from parent.
        Commit parentCommit = getCommit(parentId);
        this.trackedFiles = parentCommit.getTrackedFiles();
    }

    // merge commit
    public Commit(String msg, String parentId, String secondParentId) {
        timeStamp = new Date();
        this.parentId = parentId;
        this.secondParentId = secondParentId;
        message = msg;
        id = Utils.sha1((Object) Utils.serialize(this));
        Commit parentCommit = getCommit(parentId);
        this.trackedFiles = parentCommit.getTrackedFiles();
    }

    public String getId() {
        return id;
    }

    public Map<String, String> getTrackedFiles() {
        return trackedFiles;
    }

    public void track(String fileName, String fileId) {
        trackedFiles.put(fileName, fileId);
    }

    public String getDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z");
        return sdf.format(timeStamp);
    }

    public String getMessage() {
        return message;
    }

    public String getParent() {
        return parentId;
    }

    public String getSecondParent() {
        return secondParentId;
    };
}
