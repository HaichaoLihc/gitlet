package gitlet;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.TreeMap;

import static gitlet.Repository.*;
import static gitlet.Utils.*;

public class HelperMethods {

    private static final int ID_LENGTH = 40;

    public static boolean isInitialized() {
        return Utils.join(CWD, ".gitlet").exists();
    }

    public static File findFile(String fileName) {
        File f = Utils.join(CWD, fileName);
        if (!f.exists()) {
            System.out.println("File does not exist.");
            System.exit(0);
        }
        return f;
    }

    // hash id of a file is attained by concatenating file content byte array with file name byte array;
    public static String sha1Helper(File f) {
        byte[] concatenatedArray = concatenateByteArrays(Utils.readContents(f),
                f.getName().getBytes(StandardCharsets.UTF_8));
        return Utils.sha1((Object) concatenatedArray);
    }

    private static byte[] concatenateByteArrays(byte[] array1, byte[] array2) {
        byte[] result = new byte[array1.length + array2.length];
        System.arraycopy(array1, 0, result, 0, array1.length);
        System.arraycopy(array2, 0, result, array1.length, array2.length);
        return result;
    }

    public static void updateBranch(String branchName, String id) {
        File branchFile = Utils.join(BRANCHES, branchName);
        Utils.writeContents(branchFile, id);
    }

    public static String getIdInBranch(String branchName) {
        File branchFile = Utils.join(BRANCHES, branchName);
        if (!branchFile.exists()) {
            System.out.println("No such branch exists.");
            System.exit(0);
        }
        return Utils.readContentsAsString(branchFile);
    }

    public static String getBranchInHead() {
        return Utils.readContentsAsString(HEAD);
    }

    public static void updateHead(String branchName) {
        Utils.writeContents(HEAD, branchName);
    }

    public static TreeMap<String, String> getStage(String stageName) {
        return Utils.readObject(Utils.join(STAGE_DIR, stageName), TreeMap.class);
    }

    public static Commit getCommit(String id) {
        int len = id.length();
        if (len < ID_LENGTH) {
            for (String ids : Utils.plainFilenamesIn(COMMIT_DIR)) {
                if (ids.substring(0, len).equals(id)) {
                    id = ids;
                }
            }
        }
        File commitFile = Utils.join(COMMIT_DIR, id);
        if (!commitFile.exists()) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        return Utils.readObject(commitFile, Commit.class);
    }

    public static void saveCommit(Commit c) {
        File commit = Utils.join(COMMIT_DIR, c.getId());
        Utils.writeObject(commit, c);
    }

    public static void saveStage(String stageName, TreeMap<String, String> map) {
        File stageFile = Utils.join(STAGE_DIR, stageName);
        Utils.writeObject(stageFile, map);
    }

    public static void saveFileAsBlob(File f, String id) {
        File blobFile = Utils.join(BLOBS_DIR, id);
        Blob b = new Blob(Utils.readContents(f));
        Utils.writeObject(blobFile, b);
    }

    /** retrieve the file from a blob using blobId and put it in CWD with name outPutFileName. */
    public static void writeBlobToFile(String blobId, String outPutFileName) {
        Blob b = findBlob(blobId);
        byte[] content = b.getContents();
        writeContents(Utils.join(CWD, outPutFileName), (Object) content);
    }

    public static void clearAndSaveStage() {
        saveStage("additionStage", new TreeMap<>());
        saveStage("removalStage", new TreeMap<>());
    }

    public static String getStringFromBlob(String blobId) {
        return byteToString(findBlob(blobId).getContents());
    }

    public static String byteToString(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static Blob findBlob(String blobId) {
        File blobFile = Utils.join(BLOBS_DIR, blobId);
        if (!blobFile.exists()) {
            throw new GitletException("No blob with the ID found.");
        }
        return readObject(blobFile, Blob.class);
    }
}
