# Gitlet Design Document

**Name**: Haichao

## Classes and Data Structures

### Repository

#### Fields

1. commits (type: Tree)
2. stageForAddition (type: treeMap)
3. stageForRemoval (type: treeMap)

#### methods
 

### Commit

#### Fields

1. parent (type: Commit)
2. child (type: Commit)
3. date and time
4. commit message (string)
5. file reference and name (treeMap)

### Commands
 ... all the commands...

## Algorithms

## Persistence
### structure
- .gitlet/
    - stage/
      - additionStage (stores a treeMap)
      - removalStage (stores a treeMap)
    - HEAD (stores the id of the commit)
    - Branches/
      - main (stores the id of the commit)
      - other-branch (stores the id of the commit)
    - commits/ 
    - blobs/

### blobs
- objects that stores byte array of file content. 







