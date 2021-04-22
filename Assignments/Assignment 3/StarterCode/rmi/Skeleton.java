package rmi;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.*;

enum RMIStatus {
    OK, RMI_EXCEPTION, EXCEPTION
};

/** RMI skeleton

    <p>
    A skeleton encapsulates a multithreaded TCP server. The server's clients are
    intended to be RMI stubs created using the <code>Stub</code> class.

    <p>
    The skeleton class is parametrized by a type variable. This type variable
    should be instantiated with an interface. The skeleton will accept from the
    stub requests for calls to the methods of this interface. It will then
    forward those requests to an object. The object is specified when the
    skeleton is constructed, and must implement the remote interface. Each
    method in the interface should be marked as throwing
    <code>RMIException</code>, in addition to any other exceptions that the user
    desires.

    <p>
    Exceptions may occur at the top level in the listening and service threads.
    The skeleton's response to these exceptions can be customized by deriving
    a class from <code>Skeleton</code> and overriding <code>listen_error</code>
    or <code>service_error</code>.
*/
public class Skeleton<T>
{

    private Class<T> c;
    private T server;
    private InetSocketAddress address;
    private boolean stopped = true;
    private ServerSocket serverSocket = null;



    public Skeleton(Class<T> c, T server)
    {
        if (!c.isInterface())
        {
            throw new Error("Class does not implement any interface");
        }

        if (!isRemoteInterface(c))
        {
            throw new Error("Non-remote interface encountered.");
        }

        if(c == null)
        {
            throw new NullPointerException("Class definition cannot be null");
        }

        if(server == null)
        {
            throw new NullPointerException("Server cannot be null");
        }

        this.c = c;
        this.server = server;

    }

    /** Creates a <code>Skeleton</code> with the given initial server address.

        <p>
        This constructor should be used when the port number is significant.

        @param c An object representing the class of the interface for which the
                 skeleton server is to handle method call requests.
        @param server An object implementing said interface. Requests for method
                      calls are forwarded by the skeleton to this object.
        @param address The address at which the skeleton is to run. If
                       <code>null</code>, the address will be chosen by the
                       system when <code>start</code> is called.
        @throws Error If <code>c</code> does not represent a remote interface -
                      an interface whose methods are all marked as throwing
                      <code>RMIException</code>.
        @throws NullPointerException If either of <code>c</code> or
                                     <code>server</code> is <code>null</code>.
     */
    public Skeleton(Class<T> c, T server, InetSocketAddress address)
    {
        this(c, server);
        this.address = address;
    }

    /** Called when the listening thread exits.

        <p>
        The listening thread may exit due to a top-level exception, or due to a
        call to <code>stop</code>.

        <p>
        When this method is called, the calling thread owns the lock on the
        <code>Skeleton</code> object. Care must be taken to avoid deadlocks when
        calling <code>start</code> or <code>stop</code> from different threads
        during this call.

        <p>
        The default implementation does nothing.

        @param cause The exception that stopped the skeleton, or
                     <code>null</code> if the skeleton stopped normally.
     */
    protected void stopped(Throwable cause)
    {
    }

    /** Called when an exception occurs at the top level in the listening
        thread.

        <p>
        The intent of this method is to allow the user to report exceptions in
        the listening thread to another thread, by a mechanism of the user's
        choosing. The user may also ignore the exceptions. The default
        implementation simply stops the server. The user should not use this
        method to stop the skeleton. The exception will again be provided as the
        argument to <code>stopped</code>, which will be called later.

        @param exception The exception that occurred.
        @return <code>true</code> if the server is to resume accepting
                connections, <code>false</code> if the server is to shut down.
     */
    protected boolean listen_error(Exception exception)
    {
        return false;
    }

    /** Called when an exception occurs at the top level in a service thread.

        <p>
        The default implementation does nothing.

        @param exception The exception that occurred.
     */
    protected void service_error(RMIException exception)
    {
    }

    /** Starts the skeleton server.

        <p>
        A thread is created to listen for connection requests, and the method
        returns immediately. Additional threads are created when connections are
        accepted. The network address used for the server is determined by which
        constructor was used to create the <code>Skeleton</code> object.

        @throws RMIException When the listening socket cannot be created or
                             bound, when the listening thread cannot be created,
                             or when the server has already been started and has
                             not since stopped.
     */
    public synchronized void start() throws RMIException
    {
        if(!stopped)
        {
            System.out.println("Skeleton already started");
            return;
        }
        stopped = false;
        Listener l = new Listener();
        Thread t = new Thread(l);

        try
        {
            t.start();
            wait();
        }
        catch (Exception e)
        {
            stopped = true;
        }
    }

    /** Stops the skeleton server, if it is already running.

        <p>
        The listening thread terminates. Threads created to service connections
        may continue running until their invocations of the <code>service</code>
        method return. The server stops at some later time; the method
        <code>stopped</code> is called at that point. The server may then be
        restarted.
     */
    public synchronized void stop()
    {
        if (stopped)
        {
            return;
        }
        stopped = true;

        try
        {
            this.serverSocket.close();
        }
        catch (Exception e)
        {
            stopped = false;
        }
    }

    public synchronized InetSocketAddress getAddress()
    {
        return address;
    }

    public static boolean isRemoteInterface(Class<?> c)
    {
        for (Method method : c.getMethods())
        {
            boolean ok = false;
            for (Class<?> e : method.getExceptionTypes())
            {
                if (e.equals(RMIException.class))
                {
                    ok = true;
                    break;
                }
            }
            if (!ok)
            {
                return false;
            }
        }
        return true;
    }

    private class Listener implements Runnable
    {
        public Listener()
        {
        }

        @Override
        public void run()
        {
            synchronized (Skeleton.this)
            {
                Skeleton.this.notifyAll();

                try
                {
                    if(address == null)
                    {
                        serverSocket = new ServerSocket(0);
                        address = (InetSocketAddress) serverSocket.getLocalSocketAddress();
                    }
                    else
                    {
                        serverSocket = new ServerSocket();
                        serverSocket.bind(address);
                    }
                }
                catch (Exception e)
                {
                    if(!Skeleton.this.stopped)
                    {
                        listen_error(e);
                    }
                }
            }
            while (true)
            {
                try
                {
                    Socket socket = Skeleton.this.serverSocket.accept();
                    new Thread(new SkeletonHandler(socket)).start();
                }
                catch (Exception e)
                {
                    if (!Skeleton.this.stopped)
                    {
                        listen_error(e);
                    }
                }
                synchronized (Skeleton.this)
                {
                    if (stopped)
                    {
                        stopped(null);
                        return;
                    }
                }
            }
        }
    }

    private class SkeletonHandler implements Runnable
    {
        private Socket clientSocket;

        public SkeletonHandler(Socket clientSocket)
        {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run()
        {
            ObjectOutputStream out = null;
            Method method = null;

            try
            {
                out = new ObjectOutputStream(clientSocket.getOutputStream());
                out.flush();

                ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
                String methodName = (String) in.readObject();

                Class<?>[] parameterTypes = (Class<?>[]) in.readObject();
                Object[] args = (Object[]) in.readObject();

                try
                {
                    method = c.getMethod(methodName, parameterTypes);
                }
                catch (NoSuchMethodException e)
                {
                    try
                    {
                        out.writeObject(RMIStatus.RMI_EXCEPTION);
                        out.writeObject(e);
                        clientSocket.close();
                    }
                    catch (Exception e1)
                    {
                        service_error(new RMIException(e1.getCause()));
                    }

                }
                if (method != null)
                {
                    Object result = method.invoke(server, args);
                    try
                    {
                        out.writeObject(RMIStatus.OK);
                        out.writeObject(result);
                        clientSocket.close();
                    }
                    catch (Exception e)
                    {
                        service_error(new RMIException(e.getCause()));
                    }
                }
            }
            catch (InvocationTargetException e)
            {
                try
                {
                    out.writeObject(RMIStatus.EXCEPTION);
                    out.writeObject(e.getCause());
                }
                catch (Exception e1)
                {
                    service_error(new RMIException(e.getCause()));
                }
            }
            catch (IOException e)
            {
                service_error(new RMIException(e));
            }
            catch (Exception e)
            {
                System.out.println("Skeleton Handler exception : " + e);
                e.printStackTrace();
            }
        }
    }
}
