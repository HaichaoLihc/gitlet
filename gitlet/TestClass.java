package gitlet;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

import static gitlet.HelperMethods.*;
import static gitlet.Repository.CWD;
import static gitlet.Utils.*;

public class TestClass {

    @Test
    public void shaHashingTest() {
        Commit c = new Commit(new Date(0L), "Initial Commit");
        System.out.println(c.getId());
    }

    @Test
    public void testDate() {
        Date date1 = new Date();
        System.out.println(date1);
    }

    @Test
    public void testPlainFiles() {
        Path currentPath = Paths.get(CWD.toURI());
        Path parentPath = currentPath.getParent();
        System.out.println("parent working directory: " + parentPath);
        System.out.println("\n --- \n");
        for (String name : Utils.plainFilenamesIn(parentPath.toFile())) {
            System.out.println(name);
            System.out.println("\n --- \n");
        }
    }

    @Test
    public void testWrtingAndReading() {
        Blob b = new Blob(Utils.readContents(Utils.join(CWD, "test.txt")));
        writeObject(Utils.join(CWD, "blob"), b);
    }

    @Test
    public void testWrtingAndReading2() {
        Blob b = Utils.readObject(Utils.join(CWD, "blob"), Blob.class);
        File newTxt = Utils.join(CWD, "test.txt");
        writeContents(newTxt, (Object) b.getContents());
    }

    @Test
    public void testSha1() {
        File f1 = Utils.join(CWD, "f1");
        Utils.writeContents(f1, "first version");
        File F1 = findFile("f1");
        String id = sha1Helper(F1);
        System.out.println(id);

        Utils.writeContents(f1, "second version");
        File F2 = findFile("f1");
        String id22 = sha1Helper(F2);
        System.out.println(id22);
    }

    @Test
    public void convertByteToString() {
        File f = Utils.join(CWD, "testFile");
        Utils.writeContents(f, "hahaha");
        byte[] result = readContents(f);
        String string = new String(result, StandardCharsets.UTF_8);
        System.out.println(string);
    }

    @Test
    public void mergeBFS() {
        Commit root = new Commit(new Date(0L), "5");
        Commit three = new Commit("3", root.getId());
        Commit four = new Commit("4", root.getId());
        Commit one = new Commit("1", three.getId(), four.getId());
        Commit two = new Commit("2", four.getId());
        Commit result = MergeHelper.findSplitPoint(one, two);
        System.out.println(result);

    }

}
