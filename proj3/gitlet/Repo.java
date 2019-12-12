package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.*;

import static gitlet.Utils.*;

class Repo implements Serializable {

    Commit firstCommit;
    HashMap<String, String> _stagingArea;
    String _head; //should be a serialized commit SHA id
    ArrayList<String> _removedFiles;
    ArrayList<String> _committedRemovedFiles;
    //untracked files are those NOT in the staging area and
    //those that have been removed without having been added to begin with i.e. not in committed
    //removed files and not in the staging area
    HashMap<String, String> _branches;
    File gitlet;
    File gitletRepo;
    File commits;
    File blobs;
    File head;
    String firstCommitID;
    String currentBranch;



    Repo() {
        //making the new .gitlet directory
        gitlet = new File(".gitlet");
        gitlet.mkdir();
        //gitletRepo = new File(".gitlet/gitletRepo");
        //gitletRepo.mkdir();
        //knowing you want the .gitlet directory to have subdirectories of commits, staging area, and head, make subdirectories for these
        commits = new File(".gitlet/commits");
        commits.mkdir();
        //new directory for blobs to add the serialized version of them
        blobs = new File(".gitlet/blobs");
        blobs.mkdir();
        //new directory for head - keep head as an instance variable
        //head = new File(".gitlet/head");
        //head.mkdir();
        //(put a copy of the commit that is the head in here)

        //tracked filed kept track of by a list of file names
            //_trackedFiles = new ArrayList<String>();

        //INSTANCE VARIABLES
        //branches also a list of commits to keep track of the lists of SHA-id's of commits - should
        //be the name of the branch and it's corresponding head commit, so make it a hashmap
        //that make up a branch
        _branches = new HashMap<String, String>();

        //files that are to be removed, don't include in your next commit, take out of the hashMap
        _removedFiles = new ArrayList<String>();
        _committedRemovedFiles = new ArrayList<String>();


        //as a backup, instance variables for the staging area and head
        _stagingArea = new HashMap<String, String>();
        _head = null;

    }

    void init() {

        //makes the initial commit
            //this needs to start automatically with an initial commit - use commit class
        Date initialDate = new Date(69, Calendar.DECEMBER, 31, 16, 0, 0);

        firstCommit = new Commit("initial commit", initialDate, new HashMap<String, String>(),
                "None", true);

        //put the serialized version of this commit in the commit directory, with the name of the
        // file being it's sha-id
        firstCommitID = firstCommit.returnSHAId();
        Utils.writeObject(Utils.join(commits, firstCommitID), firstCommit);
        //make the branches just the master
        _branches.put("master", firstCommitID);
        // make this commit the head
        // should it be the name? - yes because you can
        // acces it later by looking through all the commits in the commit folder and deserializing from there
        //Utils.writeObject(Utils.join(head, firstCommitID), firstCommit);
            //getting rid of this because now head is just the sha id
        _head = firstCommitID;

        currentBranch = "master";
    }

    void add(String fileName) {
        //check if the file exists
            //what should be here?for file path name

        File checking = new File(fileName);
        if (!checking.exists()) {
            System.out.println("File does not exist.");
            //throw new GitletException("File does not exist.");
        } else {
            //making a blob of this file, where blob is the fileContents

            String blob = Utils.readContentsAsString(checking); //fileContents
            //used to be a string ^^
            byte[] serializedBlob = Utils.serialize(checking);
            String blobID = Utils.sha1((Object) blob);
                //add byte[] blob to the blob folder like it is done for commits
            Utils.writeObject(Utils.join(blobs, blobID), blob);
                //now if you want to grab this, you can call the blobID from the hashmap in the staging area
                    //Staging an already-staged file overwrites the previous entry in the staging area
                    // with the new contents. (automatically done)
            //add the blob to the stagingArea hashmap and staging area folder for addition
            //compare what you just added to the current commit's hashmap
                //do this by deserializing this commit
            File headPath = new File(".gitlet/commits/" + _head);
            Commit currentCommit = readObject(headPath, Commit.class);
            HashMap<String, String> stagingArea = currentCommit.returnStagingArea();
            if (stagingArea.containsKey(fileName)) {
                String currentBlob = stagingArea.get(fileName);
                if (!currentBlob.equals(blobID)) {
                    _stagingArea.put(fileName, blobID);
                   // Utils.writeObject(Utils.join(added, fileName), blob);
                }
            } else {
                _stagingArea.put(fileName, blobID);
               // Utils.writeObject(Utils.join(added, fileName), blob);
            }
            if (_committedRemovedFiles.contains(fileName)) {
                _committedRemovedFiles.remove(fileName);
            }
            if (_removedFiles.contains(fileName)) {
                _removedFiles.remove(fileName);
            }
        }
    }

    void commit(String message) {
        //getting initialization out of the way

        if (_stagingArea.isEmpty() && _removedFiles.isEmpty()) {
            // CHANGED: added the removed files part
            //if staging area is empty
            System.out.println("No changes added to the commit.");
            //throw new GitletException("No changes added to the commit.");
        } else if (message.equals("") || message.equals("\"\"")) {
            System.out.println("Please enter a commit message.");
        } else {

            //get the parent's staging area to combine the two
            //parent will be the old head - which you will update later
            // - it'll be the current branch's head, so use this instead
            File parentFile = new File(".gitlet/commits/" + _branches.get(currentBranch));
            Commit parentCommit = readObject(parentFile, Commit.class);
            HashMap<String, String> parentStagingArea = parentCommit.returnStagingArea();
            if (!_removedFiles.isEmpty()) {
                for (String removed : _removedFiles) {
                    parentStagingArea.remove(removed);
                    _committedRemovedFiles.add(removed);
                }
                _removedFiles.clear();
            }

            //initializing values for the new commit
            Date date = new Date();

            //create a new hashMap for the new commit
            HashMap<String, String> updatedStaging = new HashMap<String, String>();
            updatedStaging.putAll(parentStagingArea);
            updatedStaging.putAll(_stagingArea);
            //removed all the files that are "untracked" i.e. have been removed
            if (!_removedFiles.isEmpty()) {
                for (String file : _removedFiles) {
                    updatedStaging.remove(file);
                }
                //empty it out for later (to not make it redundant)
                _removedFiles.clear();
            }

            Commit newCommit = new Commit(message, date, updatedStaging, _branches.get(currentBranch), false);
            //files in the staging area are the tracked ones, become untracked later because they aren't
            //in the staging area anymore - it's cleared

            //update the head for the most recent commit of the repo
            _head = newCommit.returnSHAId();

            //delete the previous head from the head directory - just look at the _head
            //a head directory isnt needed? and instead you look
            // through the commit folder with the shaID and find it that way
            // boolean check = headFile.delete();
            // System.out.println(check);

            //add the commit to the folder of head and commits
            //Utils.writeObject(Utils.join(head, _head), newCommit); - just use the _head instance variable
            //FIXME - do i need this
            Utils.writeObject(Utils.join(commits, _head), newCommit);

            //update the branches hashmap
            _branches.put(currentBranch, _head);

            //now look through the staging area hashmap and update these files - ?? actually dont the point is just to
            //now look through the staging area hashmap and update these files - ?? actually dont the point is just to
            //keep a version of the file, you dont need to update or change any files - instead clear it out

            //CHANGED: empty out the staging area
            _stagingArea.clear();
        }

    }

    void rm(String fileName) {
        File headFile = new File (".gitlet/commits/" + _branches.get(currentBranch));
        Commit headCommit = readObject(headFile, Commit.class);
        HashMap<String, String> headStagingArea = headCommit.returnStagingArea();
        //If the file is neither staged nor tracked by the head commit, print
        // the error message
        if (!headStagingArea.containsKey(fileName) && !_stagingArea.containsKey(fileName)) {
            System.out.println("No reason to remove the file.");
        } else {
            //remove the file from the working directory if the user has not already
            //done so
            File filePath = new File(fileName);
            //Unstage the file if it is currently staged.
            if (_stagingArea.containsKey(fileName)) {
                _stagingArea.remove(fileName);
            }

            if (headStagingArea.containsKey(fileName)) {
                // If the file is tracked in the current commit, mark it
                // to indicate that it is not to be included in the next commit
                _committedRemovedFiles.add(fileName);
                _removedFiles.add(fileName);
            }
            //will be removed when you make the next commit, marked to be untracked then

            if (filePath.exists() && (headCommit.returnStagingArea().containsKey(fileName)
                    || _stagingArea.containsKey(fileName))) {
                filePath.delete();

                //The final category ("Untracked Files") is for files present
                // in the working directory but neither staged for addition nor
                // tracked. This includes files that have been staged for
                // removal, but then re-added without Gitlet's knowledge.
                // Ignore any subdirectories that may have been introduced,
                // since Gitlet does not deal with them.

                //not in the temp staging area nor in the head commit's staging area

                //FIXME - fix this delete or restricted delete
                //restrictedDelete(filePath);
            }
        }
    }

    void log() {

        //FIXME - make log work for merges

        File filePath = new File(".gitlet/commits/" + _head);
        Commit initialCommit = readObject(filePath, Commit.class);
        while (!initialCommit.returnIsFirst()) {
            if (initialCommit.returnParent().equals(initialCommit.returnSecondParent())) {
                System.out.println("===");
                System.out.println("commit " + initialCommit.returnSHAId());
                System.out.println("Date: " + initialCommit.returnDate());
                System.out.println(initialCommit.returnMessage());
                System.out.println();
                File newFilePath = new File(".gitlet/commits/" + initialCommit.returnParent());
                initialCommit = readObject(newFilePath, Commit.class);
            } else {
                //FIXME - fix this according to the merge commit thing
                System.out.println("===");
                System.out.println("commit " + initialCommit.returnSHAId());
                System.out.println("Merge:" + initialCommit.returnParent().substring(0, 7) +
                        " " + initialCommit.returnSecondParent().substring(0, 7));
                System.out.println("Date: " + initialCommit.returnDate());
                System.out.println(initialCommit.returnMessage());
                System.out.println();
                File newFilePath = new File(".gitlet/commits/" + initialCommit.returnParent());
                initialCommit = readObject(newFilePath, Commit.class);
            }
        }
        System.out.println("===");
        System.out.println("commit " + initialCommit.returnSHAId());
        System.out.println("Date: " + initialCommit.returnDate());
        System.out.println(initialCommit.returnMessage());
        System.out.println();
    }

    void globalLog() {
        List<String> commits = plainFilenamesIn(".gitlet/commits");
        for (String commit : commits) {
            File filePath = new File(".gitlet/commits/" + commit);
            Commit physical_commit = readObject(filePath, Commit.class);
            if (physical_commit.returnParent().equals(physical_commit.returnSecondParent())) {
                System.out.println("===");
                System.out.println("commit " + physical_commit.returnSHAId());
                System.out.println("Date: " + physical_commit.returnDate());
                System.out.println(physical_commit.returnMessage());
                System.out.println();
            } else {
                System.out.println("===");
                System.out.println("commit " + physical_commit.returnSHAId());
                System.out.println("Merge:" + physical_commit.returnParent().substring(0, 7) +
                        " " + physical_commit.returnSecondParent().substring(0, 7));
                //FIXME - changed this from 0 to 7 instead of 0 to 6
                System.out.println("Date: " + physical_commit.returnDate());
                System.out.println(physical_commit.returnMessage());
                System.out.println();
            }
        }
    }

    void find(String message) {
        boolean check = false;
        List<String> commits = plainFilenamesIn(".gitlet/commits");
        for (String commit : commits) {
            File filePath = new File(".gitlet/commits/" + commit);
            Commit physical_commit = readObject(filePath, Commit.class);
            //Prints out the ids of all commits that have the given commit message,
            // one per line. If there are multiple such commits, it prints the ids
            // out on separate lines.
            if (physical_commit.returnMessage().equals(message)) {
                check = true;
                System.out.println(physical_commit.returnSHAId());
            }
        }
        if (!check) {
            System.out.println("Found no commit with that message.");
        }
    }

    void status() {

        //CHANGES: made it all in alphabetical order

        //Printing out the branches
        System.out.println("=== Branches ===");
        //getting a lexicographically sorted list of the branches
        Object[] branches = _branches.keySet().toArray();
        Arrays.sort(branches);
        //checking if it's the current branch to add the *
        for (Object branch : branches) {
            if (branch.toString().equals(currentBranch)) {
                System.out.println("*" + branch);
                //if not the current branch, just print it out normally
            } else {
                System.out.println(branch);
            }
        }
        System.out.println();

        //printing out the staged files
        System.out.println("=== Staged Files ===");
        Object[] stagedFiles = _stagingArea.keySet().toArray();
        Arrays.sort(stagedFiles);
        for (Object stagedFile : stagedFiles) {
            System.out.println(stagedFile);
        }
        System.out.println();

        //printing out the removed files
        System.out.println("=== Removed Files ===");
        ArrayList<String> removedFiles = _removedFiles;
        //FIXME - changed between removed and commit removed
        Collections.sort(removedFiles);
        for (String removedFile : removedFiles) {
            System.out.println(removedFile);
        }
        System.out.println();

        System.out.println("=== Modifications Not Staged For Commit ===");
        System.out.println();
        System.out.println("=== Untracked Files ===");
        System.out.println();

    }

    void checkout(String[] arguments) {
        //java gitlet.Main checkout -- [file name]
        if (arguments.length == 2) {
            if (!arguments[0].equals("--")) {
                System.out.println("Incorrect operands.");
            } else {
                // Takes the version of the file as it exists in the head commit,
                // the front of the current branch, and puts it in the working directory,
                // overwriting the version of the file that's already there if there is one.
                // The new version of the file is not staged.

                //If the file does not exist in the previous commit, aborts,
                // printing the error message File does not exist in that commit.


                String fileName = arguments[1];
                //get the head commit
                File headPath = new File(".gitlet/commits/" + _head);
                Commit headCommit = readObject(headPath, Commit.class);
                //to test: System.out.println(headCommit.returnMessage());

                //get it's staging area and the blob with this file
                HashMap<String, String> headSA = headCommit.returnStagingArea();

                if (!headSA.containsKey(fileName)) {
                    System.out.println("File does not exist in that commit.");
                } else {
                    String blobID = headSA.get(fileName);

                    //deserialize this blob and update the file
                    File blobPath = new File(".gitlet/blobs/" + blobID);
                    String blob = readObject(blobPath, String.class);
                    File thisFile = new File(fileName);
                    Utils.writeContents(thisFile, blob);
                }
            }

         //java gitlet.Main checkout [commit id] -- [file name]
        } else if (arguments.length == 3) {
            if (!arguments[1].equals("--")) {
                System.out.println("Incorrect operands.");
            } else {

                //Takes the version of the file as it exists in the commit with the given
                // id, and puts it in the working directory, overwriting the version of
                // the file that's already there if there is one. The new version of the
                // file is not staged.

                //(same as above but gets the commit by the commit id)

                String commitID = arguments[0];
                String fileName = arguments[2];
                List<String> commits = plainFilenamesIn(".gitlet/commits");
                boolean checkForCommit = false;
                for (String commit : commits) {
                    if (commit.equals(commitID) || commit.contains(commitID)) {
                        checkForCommit = true;
                        //getting the commit with this ID
                        File filePath = new File(".gitlet/commits/" + commit);
                        Commit thisCommit = readObject(filePath, Commit.class);

                        //grab it's staging area
                        HashMap<String, String> headSA = thisCommit.returnStagingArea();

                        if (!headSA.containsKey(fileName)) {
                            System.out.println("File does not exist in that commit.");
                        } else {
                            String blobID = headSA.get(fileName);

                            //deserialize this blob and update the file
                            File blobPath = new File(".gitlet/blobs/" + blobID);
                            String blob = readObject(blobPath, String.class);
                            File thisFile = new File(fileName);
                            Utils.writeContents(thisFile, blob);
                        }
                    }
                }
                if (!checkForCommit) {
                    System.out.println("No commit with that id exists.");
                }
            }

        //java gitlet.Main checkout [branch name]
        } else if (arguments.length == 1) {
            //finished?

            //Takes all files in the commit at the head of the given branch,
            // and puts them in the working directory, overwriting the versions
            // of the files that are already there if they exist. Also, at the end
            // of this command, the given branch will now be considered the current
            // branch (HEAD). Any files that are tracked in the current branch but
            // are not present in the checked-out branch are deleted. The staging area
            // is cleared, unless the checked-out branch is the current branch
            // (see Failure cases below).

            //If no branch with that name exists, print No such branch exists.
            // If that branch is the current branch, print No need to checkout
            // the current branch. If a working file is untracked in the
            // current branch and would be overwritten by the checkout, print There
            // is an untracked file in the way; delete it or add it first. and exit;
            // perform this check before doing anything else.

            String branchName = arguments[0];

            //If a working file is untracked in the
            //current branch and would be overwritten by the checkout, print There
            //is an untracked file in the way; delete it or add it first.

            //if the file in the branchName is going to get overwritten but isn't tracked in the
            //currentBranch, throw an error
            //FIXME - this error
//            boolean check = false;
//            for (String file : givenCommit.returnStagingArea().keySet()) {
//                if (!currentHead.returnStagingArea().containsKey(file)) {
//                    check = true;
//                }
//            }

            if (!_branches.containsKey(branchName)) {
                System.out.println("No such branch exists.");
            } else if (branchName.equals(currentBranch)) {
                System.out.println("No need to checkout the current branch..");
//            } else if (check) {
//                System.out.println("There is an untracked file in the way; " +
//                        "delete it or add it first.");
                //FIXME - this error

            } else {

                String branchHeadSHA = _branches.get(branchName);

                File givenCommitPath = new File(".gitlet/commits/" + branchHeadSHA);
                Commit givenCommit = readObject(givenCommitPath, Commit.class);

                File currentHeadPath = new File(".gitlet/commits/" + _head);
                Commit currentHead = readObject(currentHeadPath, Commit.class);


                //getting the staging area for thisCommit
                HashMap<String, String> commitSA = givenCommit.returnStagingArea();

                //updating all the files in this SA
                for (String fileName : commitSA.keySet()) {
                    String blobID = commitSA.get(fileName);
                    //check if it needs to get overwritten
                    //FIXME - check here ???
                    //deserialize this blob and update the file
                    File blobPath = new File(".gitlet/blobs/" + blobID);
                    String blob = readObject(blobPath, String.class);
                    File thisFile = new File(fileName);
                    Utils.writeContents(thisFile, blob);
                }

                //Any files that are tracked in the current branch but are not present in the
                // checked-out branch are deleted.

                //get current branch head

                for (String fileName : currentHead.returnStagingArea().keySet()) {
                    if (!givenCommit.returnStagingArea().containsKey(fileName)) {
                        //delete the file
                        File deletedFile = new File(fileName);
                        restrictedDelete(deletedFile);
                    }
                }

                //staging area is cleared, unless the checked-out branch is the current branch
                if (!currentBranch.equals(branchName)) {
                    _stagingArea.clear();
                }
                currentBranch = branchName;
                _head = branchHeadSHA;
            }
        }
    }

    void branch(String branchName) {
        if (_branches.containsKey(branchName)) {
            System.out.println("A branch with that name already exists.");
        } else {
            _branches.put(branchName, _head);
        }
    }

    void rmBranch(String branchName) {
        if (!_branches.containsKey(branchName)) {
            System.out.println("A branch with that name does not exist.");
        } else if (currentBranch.equals(branchName)) {
            System.out.println("Cannot remove the current branch.");
        } else {
            _branches.remove(branchName);
        }
        //as in you cant access these
        // commits through this branch anymore?
    }

    void reset(String commitID) {

        //Description: Checks out all the files tracked by the given commit.

        String givenCommitName = "none";

        List<String> commits = plainFilenamesIn(".gitlet/commits");
        for (String commit : commits) {
            if (commit.equals(commitID)|| commit.startsWith(commitID)) {
                givenCommitName = commit;
            }
        }

        if (givenCommitName.equals("none")) {
            System.out.println("No commit with that id exists.");
        } else {

            File givenCommitPath = new File(".gitlet/commits/" + givenCommitName);
            Commit givenCommit = readObject(givenCommitPath, Commit.class);

            File currentCommit = new File(".gitlet/commits/" + _head);
            Commit current = readObject(currentCommit, Commit.class);

            boolean untrackedCheck = false;

//            for (String file : givenCommit.returnStagingArea().keySet()) {
//                if (!current.returnStagingArea().containsKey(file)
//                        && !_stagingArea.containsKey(file)) {
//                    untrackedCheck = true;
//                }
//            }
            //FIXME - got rid of this ^^

            if (untrackedCheck) {
                System.out.println("There is an untracked file in the way;" +
                        " delete it or add it first.");

                //Checks out all the files tracked by the given commit.

            } else {
                for (String file : givenCommit.returnStagingArea().keySet()) {
                    String[] checkoutArgs = new String[3];
                    checkoutArgs[0] = givenCommit.returnSHAId();
                    checkoutArgs[1] = "--";
                    checkoutArgs[2] = file;
                    checkout(checkoutArgs);
                }

                //Removes tracked files that are not present in that commit.

                for (String file : current.returnStagingArea().keySet()) {
                    if (!givenCommit.returnStagingArea().containsKey(file)) {
                        rm(file);
                    }
                }

                for (String file : _stagingArea.keySet()) {
                    if (!givenCommit.returnStagingArea().containsKey(file)) {
                        rm(file);
                    }
                }

                //Also moves the current branch's head to that commit node.
                _branches.put(currentBranch, givenCommit.returnSHAId());

                //The staging area is cleared.
                _stagingArea.clear();

            }

        }
//
//        File currentCommit = new File(".gitlet/commits/" + _head);
//        Commit current = readObject(currentCommit, Commit.class);

//        for (String file : current.returnStagingArea().keySet()) {
//            String arguments =
//            checkout(file);
//        }

        // Removes tracked files that are not present in that commit. Also
        // moves the current branch's head to that commit node. See the
        // intro for an example of what happens to the head pointer after
        // using reset. The [commit id] may be abbreviated as for checkout.
        // The staging area is cleared. The command is essentially checkout
        // of an arbitrary commit that also changes the current branch head.


//
//
//
//        //current commit things
//        File currentCommit = new File(".gitlet/commits/" + _head);
//        Commit current = readObject(currentCommit, Commit.class);
//        HashMap<String, String> tracked = current.returnStagingArea();
//        tracked.putAll(_stagingArea);
//
//        List<String> commits = plainFilenamesIn(".gitlet/commits");
//
//            boolean checkUntracked = false;
//            boolean checkForCommit = false;
//            for (String commit : commits) {
//                if (commit.equals(commitID) || commit.contains(commitID)) {
//                    checkForCommit = true;
//                    File filePath = new File(".gitlet/commits/" + commit);
//                    Commit thisCommit = readObject(filePath, Commit.class);
//
//                    for (String file : thisCommit.returnStagingArea().keySet()) {
//                        if (!current.returnStagingArea().containsKey(file)) {
//                            checkUntracked = true;
//                        }
//                    }
//
//                    if (checkUntracked) {
//                        System.out.println("There is an untracked file in the way; " +
//                                "delete it or add it first.");
//
//                        //FIXME - might need to check removed files too?
//
//                    } else {
//
//                        for (String fileName : thisCommit.returnStagingArea().keySet()) {
//                            String[] args = new String[3];
//                            args[0] = commitID;
//                            args[1] = "--";
//                            args[2] = fileName;
//                            checkout(args); //FIXME - don't just call checkout
//                        }
//
//                        for (String trackedFile : tracked.keySet()) {
//                            if (!thisCommit.returnStagingArea().containsKey(trackedFile)) {
//                                File delete = new File(trackedFile);
//                                restrictedDelete(delete);
//                            }
//                        }
//                        _branches.put(currentBranch, thisCommit.returnSHA_id());
//                        _stagingArea.clear();
//                    }
//                }
//            }

//            if (!checkForCommit) {
//                System.out.println("No commit with that id exists.");
//            }
        }



    void merge(String givenBranch) {
        if (!_stagingArea.isEmpty() || !_removedFiles.isEmpty()) {
            System.out.println("You have uncommitted changes.");
        } else if (!_branches.containsKey(givenBranch)) {
            System.out.println("A branch with that name doesn't exist.");
        } else if (givenBranch.equals(currentBranch)) {
            //if trying to merge a branch with itself, print
            // "Cannot merge a branch with itself"
            //this means the current branch
            System.out.println("Cannot merge a branch with itself.");
        //} else if (untracked) {
            //FIXME - else if that checks for untracked files too
        } else {
            //get a list of the current branch's parents and for loop through this
            //until one matches the parent of the other branch which you are also for looping through
            //check for the split point by looking for the latest common ancestor
            String splitPointID = "nothing";
            Commit splitPointCommit;

            //branchName's ancestors list
            ArrayList<String> givenBranchAncestors = new ArrayList<String>();
            String commitSHAid = _branches.get(givenBranch);
            File commitPath = new File(".gitlet/commits/" + commitSHAid);
            Commit commit = readObject(commitPath, Commit.class);
            givenBranchAncestors.add(commit.returnSHAId());
            while (!commit.returnIsFirst()) {
                //FIXME - take care of case where you have two parents need to go to
                // the second parent - is this right?
                String commitParentSHAid = commit.returnParent();
                if (!commit.returnParent().equals(commit.returnSecondParent())) {
                    commitParentSHAid = commit.returnSecondParent();
                }
                givenBranchAncestors.add(commitParentSHAid);
                File nextCommitPath = new File(".gitlet/commits/" + commitParentSHAid);
                commit = readObject(nextCommitPath, Commit.class);
            }

            //currentBranch's ancestors list
            ArrayList<String> currentBranchAncestors = new ArrayList<String>();
            String commitSHAid2 = _branches.get(currentBranch);
            File commitPath2 = new File(".gitlet/commits/" + commitSHAid2);
            Commit commit2 = readObject(commitPath2, Commit.class);
            currentBranchAncestors.add(commit2.returnSHAId());
            while (!commit2.returnIsFirst()) {
                String commitParentSHAid2 = commit.returnParent();
                if (!commit.returnParent().equals(commit.returnSecondParent())) {
                    commitParentSHAid2 = commit.returnSecondParent();
                }
                currentBranchAncestors.add(commitParentSHAid2);
                File nextCommitPath2 = new File(".gitlet/commits/" + commitParentSHAid2);
                commit2 = readObject(nextCommitPath2, Commit.class);
            }

            //now loop through the currentBranch and givenBranch ancestors until they match
            outer: for (String ancestor : givenBranchAncestors) {
                for (String currAncestor : currentBranchAncestors) {
                    if (ancestor.equals(currAncestor)) {
                        splitPointID = ancestor;
                        break outer;
                    }
                }
            }

            //FIXME - can make this a helper function later and
            // make it just return the ancestor instead of needing to break out

            //getting the split point

            File splitPointPath = new File(".gitlet/commits/" + splitPointID);
            splitPointCommit = readObject(splitPointPath, Commit.class);

            //failures

            if (splitPointID.equals(_branches.get(givenBranch))) {
                System.out.println("Given branch is an ancestor of the current branch.");
            } else if (splitPointID.equals(_branches.get(currentBranch))) {
                _branches.put(currentBranch, _branches.get(givenBranch));
                System.out.println("Current branch fast-forwarded.");
            } else {


                //the files in the given branch's staging area
                String givenBranchCommitID = _branches.get(givenBranch);
                File givenBranchCommitPath = new File(".gitlet/commits/" + givenBranchCommitID);
                Commit givenBranchCommit = readObject(givenBranchCommitPath, Commit.class);
                ArrayList<String> givenBranchFiles =
                        new ArrayList<String>(givenBranchCommit.returnStagingArea().keySet());

                String currentBranchCommitID = _branches.get(currentBranch);
                File currentBranchCommitPath = new File(".gitlet/commits/" + currentBranchCommitID);
                Commit currentBranchCommit = readObject(currentBranchCommitPath, Commit.class);
                ArrayList<String> currentBranchFiles =
                        new ArrayList<String>(currentBranchCommit.returnStagingArea().keySet());

                ArrayList<String> splitPointFiles =
                        new ArrayList<String>(splitPointCommit.returnStagingArea().keySet());

                //Any files that have been modified in the current branch but not in the
                // given branch since the split point should stay as they are.

                for (String file : givenBranchFiles) {
                    String givenBranchBlobSHA = givenBranchCommit.returnStagingArea().get(file);
                    String splitPointBlobSHA = splitPointCommit.returnStagingArea().get(file);
                    String currentBranchBlobSHA = currentBranchCommit.returnStagingArea().get(file);


                       //Any files that have been modified in the current branch but not in the
                       // given branch since the split point should stay as they are.

                    if (givenBranchBlobSHA.equals(splitPointBlobSHA)
                            && !currentBranchBlobSHA.equals(splitPointBlobSHA)) {

                        //do nothing

                        //Any files that have been modified in the given branch since the split point,
                        // but not modified in the current branch since the split point should be
                        // changed to their versions in the given branch (checked out from the commit
                        // at the front of the given branch). These files should then all be automatically
                        // staged. To clarify, if a file is "modified in the given branch since the split
                        // point" this means the version of the file as it exists in the commit at the
                        // front of the given branch has different content from the version of the file
                        // at the split point.

                    } else if (!givenBranchBlobSHA.equals(splitPointBlobSHA)) {
                        //if this is true, change them to their version in the givenBranch
                        File newBlobPath = new File(".gitlet/blobs/" + givenBranchBlobSHA);
                        String newBlob = readObject(newBlobPath, String.class);
                        File theFile = new File(file);
                        writeContents(theFile, newBlob);
                        _stagingArea.put(file, givenBranchBlobSHA);
                    }

                    //Any files that have been modified in both the current and given branch in the
                    // same way (i.e., both to files with the same content or both removed) are left
                    // unchanged by the merge.

                    else if (givenBranchBlobSHA.equals(currentBranchBlobSHA)) {
                        //FIXME - is this an issue
                    }

                    // If a file is removed in both, but a file of that name
                    // is present in the working directory that file is not removed from the working
                    // directory (but it continues to be absent—not staged—in the merge).

                    else if ((!givenBranchCommit.returnStagingArea().containsKey(file)
                            && !currentBranchCommit.returnStagingArea().containsKey(file))
                            && splitPointCommit.returnStagingArea().containsKey(file)) {
                        //should this be here? _stagingArea.put(file, givenBranchBlobSHA);
                        File checkedFile = new File(file);
                        if ((checkedFile.exists())) {
                            //make it not staged by making sure it's not in the staging area
                            //FIXME - don't do anything?, look above comment
                        }

                    //Any files that were not present at the split point and are present only in the
                    //current branch should remain as they are.

                    } else if (!splitPointCommit.returnStagingArea().containsKey(file)
                            && !givenBranchCommit.returnStagingArea().containsKey(file)
                            && currentBranchCommit.returnStagingArea().containsKey(file)) {
                        //should remain as they are - FIXME - what does this mean?
                        //do nothing? - put it in the staging area as it is currently
                        _stagingArea.put(file, currentBranchCommit.returnStagingArea().get(file));
                    }

                    //Any files that were not present at the split point and are present only
                    // in the given branch should be checked out and staged.

                    else if (!splitPointCommit.returnStagingArea().containsKey(file)
                            && givenBranchCommit.returnStagingArea().containsKey(file)
                            && !currentBranchCommit.returnStagingArea().containsKey(file)) {
                        //checking out
                        File newBlobPath = new File(".gitlet/blobs/" + givenBranchBlobSHA);
                        String newBlob = readObject(newBlobPath, String.class);
                        File theFile = new File(file);
                        writeContents(theFile, newBlob);
                        //staging
                        _stagingArea.put(file, givenBranchBlobSHA);
                    }

                    //Any files present at the split point, unmodified in the current branch,
                    // and absent in the given branch should be removed (and untracked).

                    else if (splitPointCommit.returnStagingArea().containsKey(file)
                            && splitPointCommit.returnStagingArea().get(file).equals(
                            currentBranchCommit.returnStagingArea().get(file))
                            && !givenBranchCommit.returnStagingArea().containsKey(file)) {
                                _stagingArea.remove(file);
                                _removedFiles.add(file);
                     }

                    //Any files present at the split point, unmodified in the given branch,
                    // and absent in the current branch should remain absent.

                    else if (splitPointCommit.returnStagingArea().containsKey(file)
                            && splitPointCommit.returnStagingArea().get(file).equals(
                            givenBranchCommit.returnStagingArea().get(file))
                            && !currentBranchCommit.returnStagingArea().containsKey(file)) {
                        _stagingArea.remove(file);
                    }

                    //Any files modified in different ways in the current and given branches are in
                    // conflict. "Modified in different ways" can mean that the contents of both are
                    // changed and different from other, or the contents of one are changed and the
                    // other file is deleted, or the file was absent at the split point and has different
                    // contents in the given and current branches. In this case, replace the contents of
                    // the conflicted file with

                    //<<<<<<< HEAD
                    //contents of file in current branch
                    //=======
                    // contents of file in given branch
                    // >>>>>>>

                    // might end up with something like this too:

                    //<<<<<<< HEAD
                    //contents of file in current branch=======
                    //contents of file in given branch>>>>>>>

                    else {
                        String beginning = "<<<<<<< HEAD";
                        String currentBlob = currentBranchCommit.returnStagingArea().get(file);
                        String middle = "=======";
                        String givenBlob = givenBranchCommit.returnStagingArea().get(file);
                        String end = ">>>>>>>";

                        String[] updatedContents = new String[5];
                        updatedContents[0] = beginning;
                        updatedContents[1] = currentBlob;
                        updatedContents[2] = middle;
                        updatedContents[3] = givenBlob;
                        updatedContents[4] = end;

                        File filePath = new File(file);
                        writeContents(filePath, (Object) updatedContents);

                        System.out.println("Encountered a merge conflict");

                    }

                }

                //Once files have been updated according to the above, and the
                // split point was not the current branch or the given branch, merge
                // automatically commits with the log message Merged [given branch name]
                // into [current branch name]. Then, if the merge encountered a conflict,
                // print the message Encountered a merge conflict. on the terminal (not the log).
                // Merge commits differ from other commits: they record as parents both the head
                // of the current branch (called the first parent) and the head of the branch
                // given on the command line to be merged in.

                //changing log message
                givenBranchCommit.changeMessage("Merged" + givenBranch + "into" + currentBranch + ".");
                //changing second parent
                givenBranchCommit.changeSecondParent(currentBranchCommit.returnParent());

            }

            }

        }





    }


//FIXME - untracked means it's never been in the staging area or the removed files list

//look at fixmes