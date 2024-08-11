package gitlet;

import com.sun.source.tree.Tree;

import java.io.File;
import java.util.*;

import static gitlet.HelperMethods.getBranchInHead;
import static gitlet.Utils.*;
import static gitlet.HelperMethods.*;
import static gitlet.MergeHelper.*;

/** Represents a gitlet repository.
 *  contains static methods of git commands.
 *  @author Haichao
 */
public class Repository {
    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));

    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");

    public static final File COMMIT_DIR = join(GITLET_DIR, "commits");

    public static final File BLOBS_DIR = join(GITLET_DIR, "blobs");

    public static final File STAGE_DIR = join(GITLET_DIR, "stage");

    public static final File ADDITION_STAGE = join(STAGE_DIR, "additionStage");

    public static final File REMOVAL_STAGE = join(STAGE_DIR, "removalStage");

    public static final File HEAD = join(GITLET_DIR, "HEAD");

    public static final File BRANCHES = join(GITLET_DIR, "branches");

    public static void init() {
        if (GITLET_DIR.exists()) {
            System.out.println("A Gitlet version-control system already exists in the current directory.");
            System.exit(0);
        }
        GITLET_DIR.mkdir();
        COMMIT_DIR.mkdir();
        BLOBS_DIR.mkdir();
        STAGE_DIR.mkdir();
        BRANCHES.mkdir();
        Utils.writeObject(ADDITION_STAGE, new TreeMap<String, String>());
        Utils.writeObject(REMOVAL_STAGE, new TreeMap<String, String>());
        Commit commitZero = new Commit(new Date(0L), "initial commit");
        saveCommit(commitZero);
        updateHead("main");
        updateBranch("main", commitZero.getId());
    }

    public static void add(String fileName) {
        TreeMap<String, String> additionStage = getStage("additionStage");
        TreeMap<String, String> removalStage = getStage("removalStage");
        Map<String, String> trackedFiles = getCommit(getIdInBranch(getBranchInHead())).getTrackedFiles();
        File f = findFile(fileName);
        String id = sha1Helper(f);
        // If the current file matches the commit version, do not stage it or remove it from staging if already staged.
        if (removalStage.containsValue(id)) {
            removalStage.remove(fileName);
        } else if (!additionStage.containsValue(id) && !trackedFiles.containsValue(id)) {
            additionStage.put(fileName, id);
        } else if (additionStage.containsKey(fileName) && trackedFiles.containsValue(id)) {
            additionStage.remove(fileName);
        }

        saveStage("additionStage", additionStage);
        saveStage("removalStage", removalStage);
        saveFileAsBlob(f, id);
    }

    // head is always pointing to the parent commit.
    public static void commit(String msg) {
        // initialized commit and read the maps
        Commit c = new Commit(msg, getIdInBranch(getBranchInHead()));
        TreeMap<String, String> additionStageFiles = getStage("additionStage");
        TreeMap<String, String> removalStageFiles = getStage("removalStage");
        Map<String, String> trackedFiles = c.getTrackedFiles();

        if (additionStageFiles.isEmpty() && removalStageFiles.isEmpty()) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }

        // iterate over the file names of addition stage
        // if found same files, check the id. If id is different, update the tracked files.
        for (String key : additionStageFiles.keySet()) {
            if (trackedFiles.containsKey(key)) {
                if (!trackedFiles.get(key).equals(additionStageFiles.get(key))) {
                    trackedFiles.put(key, additionStageFiles.get(key));
                }
            } else {
                trackedFiles.put(key, additionStageFiles.get(key));
            }
        }

        // iterate over files names of remove stage
        // remove the files in tracked files of the commit
        for (String key : removalStageFiles.keySet()) {
            trackedFiles.remove(key);
        }

        // clear both stages, save the new commit, and advance MAIN.
        clearAndSaveStage();
        saveCommit(c);
        updateBranch(getBranchInHead(), c.getId());
    }

    public static void rm(String fileName) {
        TreeMap<String, String> additionStageFiles = getStage("additionStage");
        TreeMap<String, String> removalStageFiles = getStage("removalStage");
        Map<String, String> trackedFiles = getCommit(getIdInBranch(getBranchInHead())).getTrackedFiles();
        String id = trackedFiles.get(fileName);

        File f = Utils.join(CWD, fileName);
        // If the file is not staged for addition and not tracked by the head commit
        if (!additionStageFiles.containsKey(fileName) && !trackedFiles.containsKey(fileName)) {
            System.out.println("No reason to remove the file.");
            System.exit(0);
        }
        // If the file is staged for addition, unstage it
        if (additionStageFiles.containsKey(fileName)) {
            additionStageFiles.remove(fileName);
        }
        // if the file is already deleted, stage it for removal.
        // If the file is tracked in the current commit, stage it for removal and remove it from the working directory
        if (!f.exists()) {
            removalStageFiles.put(fileName, id);
        } else if (trackedFiles.containsKey(fileName)) {
            removalStageFiles.put(fileName, id);
            f.delete();
        }
        saveStage("additionStage", additionStageFiles);
        saveStage("removalStage", removalStageFiles);
    }

    public static void log() {
        Commit p = getCommit(getIdInBranch(getBranchInHead()));
        StringBuilder fullString = new StringBuilder();

        // traverse the commit linked list
        while (true) {
            fullString.append(commitInfo(p));
            if (p.getParent() == null) {
                break;
            }
            p = getCommit(p.getParent());
        }
        System.out.println(fullString);
    }

    private static StringBuilder commitInfo(Commit c) {
        StringBuilder fullString = new StringBuilder();
        fullString.append("===\n");
        fullString.append("commit ").append(c.getId()).append("\n");
        if (c.getSecondParent() != null) {
            fullString.append("Merge: ").append(c.getParent(), 0, 7).append(" ")
                    .append(c.getSecondParent(), 0, 7).append("\n");
        }
        fullString.append("Date: ").append(c.getDate()).append("\n");
        fullString.append(c.getMessage());
        fullString.append("\n\n");
        return fullString;
    }


    public static void globalLog() {
        StringBuilder fullString = new StringBuilder();
        fullString.append("\n\n");
        for (String id : Utils.plainFilenamesIn(COMMIT_DIR)) {
            Commit c = getCommit(id);
            fullString.append(commitInfo(c));
        }
        System.out.println(fullString);
    }

    public static void find(String arg) {
        StringBuilder fullString = new StringBuilder();
        for (String id : Utils.plainFilenamesIn(COMMIT_DIR)) {
            Commit c = getCommit(id);
            if (c.getMessage().equals(arg)) {
                fullString.append(id).append("\n");
            }
        }
        if (fullString.isEmpty()) {
            System.out.println("Found no commit with that message.");
            System.exit(0);
        }
        System.out.println(fullString);
    }

    public static void status() {
        StringBuilder fullString = new StringBuilder();
        fullString.append("=== Branches ===").append("\n");
        // iterate over all branches file.

        for (String fileName : Utils.plainFilenamesIn(BRANCHES)) {
                if (fileName.equals(getBranchInHead())) {
                    fullString.append("*").append(fileName).append("\n");
                } else {
                    fullString.append(fileName).append("\n");
                }
        }
        fullString.append("\n=== Staged Files ===\n");
        // iterate over addition stage.
        for (String fileName : getStage("additionStage").keySet()) {
            fullString.append(fileName).append("\n");
        }
        fullString.append("\n=== Removed Files ===\n");
        // iterate over removal stage.
        for (String fileName : getStage("removalStage").keySet()) {
            fullString.append(fileName).append("\n");
        }
        fullString.append("\n=== Modifications Not Staged For Commit ===\n");
        // iterate over files in CWD
        //        Map<String, String> trackedFiles = getCommit(getIdInHead()).getTrackedFiles();
        //        for (String fileName : Utils.plainFilenamesIn(CWD)) {
        //            if (sha1Helper(findFile(fileName)));
        //        }
        fullString.append("\n=== Untracked Files ===\n");
        System.out.println(fullString);
    }

    public static void restore(String commitId, String fileName) {
        Commit c;
        if (commitId == null) {
            c = getCommit(getIdInBranch(getBranchInHead()));
        } else {
            c = getCommit(commitId);
        }
        String blobId = c.getTrackedFiles().get(fileName);
        if (blobId == null) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
        writeBlobToFile(blobId, fileName);
    }

    public static void branch(String branchName) {
        File newBranch = Utils.join(BRANCHES, branchName);
        if (newBranch.exists()) {
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        }
        Utils.writeContents(newBranch, getIdInBranch(getBranchInHead()));

    }

    public static void switchBranch(String branchName) {
        Commit destCommit = getCommit(getIdInBranch(branchName));
        Commit sourceCommit = getCommit(getIdInBranch(getBranchInHead()));
        if (destCommit == sourceCommit) {
            System.out.println("No need to switch to the current branch.");
            System.exit(0);
        }
        if (untrackedFileExist(destCommit)) {
            System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
            System.exit(0);
        }
        replaceTrackedFiles(destCommit, sourceCommit);
        updateHead(branchName);
    }

    /** return true if a working file is untracked in the current branch and would be overwritten by the switch */
    private static boolean untrackedFileExist(Commit destCommit) {
        Commit sourceCommit = getCommit(getIdInBranch(getBranchInHead()));
        Map<String, String> sourceTrackedFiles = sourceCommit.getTrackedFiles();
        Map<String, String> destCommitFiles = destCommit.getTrackedFiles();
        for (String fileName : Utils.plainFilenamesIn(CWD)) {
            if (!sourceTrackedFiles.containsKey(fileName) && destCommitFiles.containsKey(fileName)
                    && Utils.join(CWD, fileName).exists()) {
                return true;
            }
        }
        return false;
    }

    /** replace the CWD with tracked files in destCommit,
     * delete files in CWD that is tracked by current commit but not in tracked files of destCommit. */
    private static void replaceTrackedFiles(Commit destCommit, Commit sourceCommit) {
        Map<String, String> destTrackedFiles = destCommit.getTrackedFiles();
        Map<String, String> sourceTrackedFiles = sourceCommit.getTrackedFiles();
        for (String fileName : sourceTrackedFiles.keySet()) {
            if (!destTrackedFiles.containsKey(fileName)) {
                Utils.join(CWD, fileName).delete();
            }
        }
        for (String fileName : destTrackedFiles.keySet()) {
            writeBlobToFile(destTrackedFiles.get(fileName), fileName);
        }
    }

    public static void removeBranch(String branchName) {
        File branchFile = Utils.join(BRANCHES, branchName);
        if (!branchFile.exists()) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        if (getBranchInHead().equals(branchName)) {
            System.out.println("Cannot remove the current branch.");
            System.exit(0);
        }
        branchFile.delete();
    }

    public static void reset(String commitId) {
        Commit destCommit;
        if (commitId == null) {
            destCommit = getCommit(getIdInBranch(getBranchInHead()));
        } else {
            destCommit = getCommit(commitId);
        }
        Commit sourceCommit = getCommit(getIdInBranch(getBranchInHead()));
        if (untrackedFileExist(destCommit)) {
            System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
            System.exit(0);
        }
        replaceTrackedFiles(destCommit, sourceCommit);
        updateBranch(getBranchInHead(), commitId);
        clearAndSaveStage();
    }

    public static void merge(String otherBranch) {
        if (!Utils.join(BRANCHES, otherBranch).exists()) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }

        String headBranch = getBranchInHead();
        if (headBranch.equals(otherBranch)) {
            System.out.println("Cannot merge a branch with itself.");
            System.exit(0);
        }

        TreeMap<String, String> additionStageFiles = getStage("additionStage");
        TreeMap<String, String> removalStageFiles = getStage("removalStage");
        if (!additionStageFiles.isEmpty() || !removalStageFiles.isEmpty()) {
            System.out.println("You have uncommitted changes.");
            System.exit(0);
        }

        Commit headCommit = getCommit(getIdInBranch(headBranch));
        Commit otherHeadCommit = getCommit(getIdInBranch(otherBranch));

        if (untrackedFileExist(otherHeadCommit)) {
            System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
            System.exit(0);
        }

        Commit splitCommit = findSplitPoint(headCommit, otherHeadCommit);

        if (headCommit.getId().equals(splitCommit.getId())) {
            updateBranch(headBranch, otherHeadCommit.getId());
            updateHead(headBranch);
            replaceTrackedFiles(otherHeadCommit, headCommit);
            System.out.println("Current branch fast-forwarded.");
            System.exit(0);
        }
        if (otherHeadCommit.getId().equals(splitCommit.getId())) {
            System.out.println("Given branch is an ancestor of the current branch.");
            System.exit(0);
        }

        Commit c = new Commit("Merged " + otherBranch + " into " + getBranchInHead() + ".",
                headCommit.getId(), otherHeadCommit.getId());

        // creates copies of tracked file map.
        Map<String, String> newTrackedFiles = c.getTrackedFiles();
        Map<String, String> headFiles = new HashMap<>(headCommit.getTrackedFiles());
        Map<String, String> otherFiles = new HashMap<>(otherHeadCommit.getTrackedFiles());
        Map<String, String> splitFiles = splitCommit.getTrackedFiles();

        for (String fileName : splitFiles.keySet()) {
            // if both heads contain the same file name.
            if (headFiles.containsKey(fileName) && otherFiles.containsKey(fileName)) {
                // if file content in both head are modified in the same way
                if (headFiles.get(fileName).equals(otherFiles.get(fileName))
                        && !headFiles.get(fileName).equals(splitFiles.get(fileName))) {
                    // do nothing
                }
                // if head modified the file but other head didn't.
                if (otherFiles.get(fileName).equals(splitFiles.get(fileName))
                        && !headFiles.get(fileName).equals(splitFiles.get(fileName))) {
                    // do nothing
                }
                // if other head modified the file but head didn't.
                if (headFiles.get(fileName).equals(splitFiles.get(fileName))
                        && !otherFiles.get(fileName).equals(splitFiles.get(fileName))) {
                    restore(otherHeadCommit.getId(), fileName);
                    newTrackedFiles.put(fileName, otherFiles.get(fileName));
                }
                // if other head and head modified the same file in different ways
                if (!otherFiles.get(fileName).equals(splitFiles.get(fileName))
                        && !headFiles.get(fileName).equals(splitFiles.get(fileName))
                        && !headFiles.get(fileName).equals(otherFiles.get(fileName))) {
                    mergeConflict(fileName, otherFiles.get(fileName));
                }
                headFiles.remove(fileName);
                otherFiles.remove(fileName);
            } else if (headFiles.containsKey(fileName) && headFiles.get(fileName).equals(splitFiles.get(fileName))
                    && !otherFiles.containsKey(fileName)) {
                // files unmodified in the current branch,
                // and absent in the given branch should be removed (and untracked).
                Utils.join(CWD, fileName).delete();
                newTrackedFiles.remove(fileName, splitFiles.get(fileName));
                headFiles.remove(fileName);
            } else if (otherFiles.containsKey(fileName) && otherFiles.get(fileName).equals(splitFiles.get(fileName))
                    && !headFiles.containsKey(fileName)) {
                // files unmodified in the given branch,
                // and absent in the current branch should remain absent.
                // delete the file that is in the split point in our temporary map
                otherFiles.remove(fileName);
            } else if (headFiles.containsKey(fileName) && !headFiles.get(fileName).equals(splitFiles.get(fileName))
                    && !otherFiles.containsKey(fileName)) {
                // files modified in the current branch,
                // and absent in the given branch
                mergeConflict(fileName, null);
            } else if (otherFiles.containsKey(fileName) && !otherFiles.get(fileName).equals(splitFiles.get(fileName))
                    && !headFiles.containsKey(fileName)) {
                // files modified in the given branch,
                // and absent in the current branch
                mergeConflict(fileName, null);
            }
        }
        // files not present at the split point and only in the given branch should be restored and staged.
        for (String fileName : otherFiles.keySet()) {
            if (!headFiles.containsKey(fileName)) {
                restore(otherHeadCommit.getId(), fileName);
                newTrackedFiles.put(fileName, otherFiles.get(fileName));
            } else if (!Objects.equals(headFiles.get(fileName), otherFiles.get(fileName))) {
                // if file not present at the split have same file name
                // but have different content in current and other commit.
                mergeConflict(fileName, otherFiles.get(fileName));
            }
        }
        clearAndSaveStage();
        saveCommit(c);
        updateBranch(getBranchInHead(), c.getId());
    }

    private static void mergeConflict(String currFileName, String otherFileId) {
        System.out.println("Encountered a merge conflict.");
        File f = findFile(currFileName);
        if (f.exists()) {
            String otherFileContent;
            if (otherFileId == null) {
                otherFileContent = "";
            } else {
                otherFileContent = getStringFromBlob(otherFileId);
            }
            String currFileContent = Utils.readContentsAsString(f);
            String result = "<<<<<<< HEAD\n" + currFileContent + "=======\n" + otherFileContent + ">>>>>>>\n";
            writeContents(f, result);
        }
    }
}
