package naming;

import common.Path;
import rmi.RMIException;

import java.io.FileNotFoundException;
import java.util.*;


enum Operation
{
    CREATEDIR, CREATEFILE, DELETE, ISDIR
}

public class HashTree
{
    public HashNode root;
    private LinkedList<ServerStub> serverStubList;


    public HashTree (LinkedList<ServerStub> serverStubList)
    {
        this.serverStubList = serverStubList;
        this.root = new HashNode();
    }


    public String[] list (Path directory) throws FileNotFoundException
    {

        Iterator<String> iter = directory.iterator();
        String fileName;
        HashNode subRoot = root;

        while (iter.hasNext())
        {
            fileName = iter.next();
            if(!subRoot.hasDirectory(fileName))
                throw new FileNotFoundException();

            subRoot = subRoot.getChild(fileName);
        }

        if (subRoot.hashtable == null)
        {
            throw new FileNotFoundException("File is not a directory");
        }

        String[] fileList = new String[subRoot.hashtable.keySet().size()];
        fileList = subRoot.hashtable.keySet().toArray(fileList);

        return fileList;
    }


    public boolean createDirectory (Path directory) throws FileNotFoundException
    {
        Iterator<String> iter = directory.iterator();
        boolean flag = OperationHelper(Operation.CREATEDIR, root, iter, null, directory);
        return flag;
    }


    public boolean createFile (Path file, ServerStub serverStub) throws FileNotFoundException
    {

        Iterator<String> iter = file.iterator();
        boolean flag = OperationHelper(Operation.CREATEFILE, root, iter, serverStub, file);

        return flag;
    }


    public boolean createFileRecursive(Path path, ServerStub serverStub)
    {
        Iterator<String> iter = path.iterator();
        HashNode hashNode = root;
        String currentPath = iter.next();
        boolean doesNotExists = true;

        while (iter.hasNext())
        {
            if (hashNode.hasFile(currentPath))
            {
                doesNotExists = false;
                break;
            }

            if (!hashNode.hasDirectory(currentPath))
            {
                hashNode.create(currentPath, null);
            }

            hashNode = hashNode.getChild(currentPath);
            currentPath = iter.next();
        }

        if (doesNotExists)
        {
            if (hashNode.hasDirectory(currentPath) || hashNode.hasFile(currentPath))
            {
                doesNotExists = false;
            }
            else
            {
                hashNode.create(currentPath, serverStub);
            }
        }


        return doesNotExists;

    }


    public boolean delete (Path path) throws FileNotFoundException
    {

        Iterator<String> iter = path.iterator();
        boolean flag = OperationHelper(Operation.DELETE, root, iter, null, path);

        if (!flag)
        {
            throw new FileNotFoundException("File not found in the server stubs available");
        }

        return flag;
    }


    public boolean isDirectory (Path path) throws FileNotFoundException
    {

        Iterator<String> iter = path.iterator();
        boolean flag = OperationHelper(Operation.ISDIR, root, iter, null, path);

        return flag;
    }

    public boolean OperationHelper(Operation mode, HashNode root, Iterator<String> iter, ServerStub serverStub, Path path) throws FileNotFoundException
    {
        if(!iter.hasNext())
        {
            if (mode == Operation.CREATEDIR || mode == Operation.CREATEFILE || mode == Operation.DELETE)
            {
                return false;
            }

            if (mode == Operation.ISDIR)
                return true;
        }

        String nextDir = iter.next();

        if (iter.hasNext())
        {
            return OperationHelper(mode, root.getChild(nextDir), iter, serverStub, path);
        }
        else
        {
            if (mode == Operation.ISDIR)
            {
                if (root.hasDirectory(nextDir))
                    return true;
                if (root.hasFile(nextDir))
                    return false;

                throw new FileNotFoundException("File not found in the directory in any storage server");
            }
            else
            {
                if (root.hasDirectory(nextDir) || root.hasFile(nextDir))
                {
                    if (mode == Operation.DELETE)
                    {
                        root.delete (nextDir, path);
                        return true;
                    }
                    else
                    {
                        return false;
                    }
                }
                else
                {
                    if (mode == Operation.DELETE)
                    {
                        return false;
                    }
                    else
                    {
                        root.create(nextDir, serverStub);
                        return true;
                    }
                }
            }
        }
    }


    public ServerStub getStorage (Path path) throws FileNotFoundException
    {
        Iterator<String> iter = path.iterator();
        HashNode subRoot = root;
        String name;

        while (iter.hasNext())
        {
            name = iter.next();
            if (iter.hasNext())
            {
                if (subRoot.hasDirectory(name))
                {
                    subRoot = subRoot.getChild(name);
                }
                else
                {
                    throw new FileNotFoundException("Directory is invalid");
                }
            }
            else
            {
                if (subRoot.hasFile(name))
                {
                    return subRoot.getFileStorage (name);
                }
                else
                {
                    throw new FileNotFoundException("File not found in the given directory");
                }
            }
        }

        return null;
    }



    private class HashNode {

        private int serverIndex = 0;
        private LinkedList<ServerStub> serverStubList = null;
        private Hashtable<String, HashNode> hashtable = null;

        public HashNode()
        {
            hashtable = new Hashtable<>();
        }


        public HashNode (ServerStub serverStub)
        {
            this.serverStubList = new LinkedList<>();
            this.serverStubList.add(serverStub);
        }


        public boolean hasDirectory (String rootDir)
        {
            HashNode node = this.hashtable.get(rootDir);

            if (node != null && node.hashtable != null)
                return true;

            return false;
        }


        public boolean hasFile (String filename)
        {
            HashNode hashNode = hashtable.get(filename);

            if (hashNode != null && hashNode.serverStubList != null)
                return true;

            return false;
        }


        public HashNode getChild (String dir)
        {
            return hashtable.get(dir);
        }


        public ServerStub getFileStorage(String filename)
        {
            HashNode hashNode = this.hashtable.get(filename);
            hashNode.serverIndex++;

            return hashNode.serverStubList.get(hashNode.serverIndex % hashNode.serverStubList.size());
        }


        public void create(String fileName, ServerStub serverStub)
        {
            HashNode node;

            if (serverStub == null)
            {
                node = new HashNode();
            }
            else
            {
                node = new HashNode(serverStub);
            }
            this.hashtable.put(fileName, node);

        }


        public void delete(String filename, Path path)
        {
            HashNode child = this.getChild(filename);

            for (ServerStub serverStub : child.getAllStubs())
            {
                try
                {
                    serverStub.commandStub.delete(path);
                }
                catch (RMIException e)
                {
                    e.printStackTrace();
                }
            }
            this.hashtable.remove(filename);
        }

        public HashSet<ServerStub> getAllStubs()
        {
            if (this.hashtable == null)
            {
                return new HashSet<>(this.serverStubList);
            }
            HashSet<ServerStub> serverStubs = new HashSet<>();

            for (HashNode hashNode : hashtable.values())
            {
                serverStubs.addAll(hashNode.getAllStubs());
            }
            return serverStubs;
        }
    }
}


