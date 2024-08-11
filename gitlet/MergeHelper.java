package gitlet;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

import static gitlet.HelperMethods.*;

public class MergeHelper {

    /** find the common split point commit object given two commit objects */
    public static Commit findSplitPoint(Commit pointer1, Commit pointer2) {
        HashSet<String> branch1Records = new HashSet<>();
        Queue<Commit> queue = new LinkedList<>();

        queue.add(pointer1);
        while (!queue.isEmpty()) {
            Commit current = queue.poll();
            if (current != null && branch1Records.add(current.getId())) {
                if (current.getParent() != null) {
                    queue.add(getCommit(current.getParent()));
                }
                if (current.getSecondParent() != null) {
                    queue.add(getCommit(current.getSecondParent()));
                }
            }
        }

        queue.add(pointer2);
        while (!queue.isEmpty()) {
            Commit current = queue.poll();
            if (current != null) {
                if (branch1Records.contains(current.getId())) {
                    return current;
                }
                if (current.getParent() != null) {
                    queue.add(getCommit(current.getParent()));
                }
                if (current.getSecondParent() != null) {
                    queue.add(getCommit(current.getSecondParent()));
                }
            }
        }

        // If no common ancestor is found, return null
        return null;
    }
}
