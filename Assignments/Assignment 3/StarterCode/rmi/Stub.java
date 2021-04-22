package rmi;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.*;

/** RMI stub factory.

    <p>
    RMI stubs hide network communication with the remote server and provide a
    simple object-like interface to their users. This class provides methods for
    creating stub objects dynamically, when given pre-defined interfaces.

    <p>
    The network address of the remote server is set when a stub is created, and
    may not be modified afterwards. Two stubs are equal if they implement the
    same interface and carry the same remote server address - and would
    therefore connect to the same skeleton. Stubs are serializable.
 */
public abstract class Stub
{
    /** Creates a stub, given a skeleton with an assigned adress.

        <p>
        The stub is assigned the address of the skeleton. The skeleton must
        either have been created with a fixed address, or else it must have
        already been started.

        <p>
        This method should be used when the stub is created together with the
        skeleton. The stub may then be transmitted over the network to enable
        communication with the skeleton.

        @param c A <code>Class</code> object representing the interface
                 implemented by the remote object.
        @param skeleton The skeleton whose network address is to be used.
        @return The stub created.
        @throws IllegalStateException If the skeleton has not been assigned an
                                      address by the user and has not yet been
                                      started.
        @throws UnknownHostException When the skeleton address is a wildcard and
                                     a port is assigned, but no address can be
                                     found for the local host.
        @throws NullPointerException If any argument is <code>null</code>.
        @throws Error If <code>c</code> does not represent a remote interface
                      - an interface in which each method is marked as throwing
                      <code>RMIException</code>, or if an object implementing
                      this interface cannot be dynamically created.
     */
    public static <T> T create(Class<T> c, Skeleton<T> skeleton)
        throws UnknownHostException
    {
        InetSocketAddress address = skeleton.getAddress();
        if (address == null)
        {
            throw new IllegalStateException("skeleton has not been assigned an address");
        }
        return create(c, address);
    }

    /** Creates a stub, given a skeleton with an assigned address and a hostname
        which overrides the skeleton's hostname.

        <p>
        The stub is assigned the port of the skeleton and the given hostname.
        The skeleton must either have been started with a fixed port, or else
        it must have been started to receive a system-assigned port, for this
        method to succeed.

        <p>
        This method should be used when the stub is created together with the
        skeleton, but firewalls or private networks prevent the system from
        automatically assigning a valid externally-routable address to the
        skeleton. In this case, the creator of the stub has the option of
        obtaining an externally-routable address by other means, and specifying
        this hostname to this method.

        @param c A <code>Class</code> object representing the interface
                 implemented by the remote object.
        @param skeleton The skeleton whose port is to be used.
        @param hostname The hostname with which the stub will be created.
        @return The stub created.
        @throws IllegalStateException If the skeleton has not been assigned a
                                      port.
        @throws NullPointerException If any argument is <code>null</code>.
        @throws Error If <code>c</code> does not represent a remote interface
                      - an interface in which each method is marked as throwing
                      <code>RMIException</code>, or if an object implementing
                      this interface cannot be dynamically created.
     */
    public static <T> T create(Class<T> c, Skeleton<T> skeleton,
                               String hostname)
    {
        InetSocketAddress address = skeleton.getAddress();
        if (address == null)
        {
            throw new IllegalStateException("skeleton has not been assigned an address");
        }
        if (hostname == null)
        {
            throw new NullPointerException("Hostname is null");
        }
        address = new InetSocketAddress(hostname, address.getPort());
        return create(c, address);
    }

    /** Creates a stub, given the address of a remote server.

        <p>
        This method should be used primarily when bootstrapping RMI. In this
        case, the server is already running on a remote host but there is
        not necessarily a direct way to obtain an associated stub.

        @param c A <code>Class</code> object representing the interface
                 implemented by the remote object.
        @param address The network address of the remote skeleton.
        @return The stub created.
        @throws NullPointerException If any argument is <code>null</code>.
        @throws Error If <code>c</code> does not represent a remote interface
                      - an interface in which each method is marked as throwing
                      <code>RMIException</code>, or if an object implementing
                      this interface cannot be dynamically created.
     */
    @SuppressWarnings("unchecked")
    public static <T> T create(Class<T> c, InetSocketAddress address)
    {
        if (c == null)
        {
            throw new NullPointerException("Class is null");
        }
        if (address == null)
        {
            throw new NullPointerException("Address is null");
        }
        if (!c.isInterface())
        {
            throw new Error("Class does not implement any interface.");
        }
        if (!Skeleton.isRemoteInterface(c))
        {
            throw new Error("Non-remote interface encountered.");
        }
        try
        {
            return (T) Proxy.newProxyInstance(c.getClassLoader(), new Class<?>[] { c }, new StubHandler(c, address));
        }
        catch (Exception e)
        {
            System.out.println(e);
            return null;
        }
    }

    private static class StubHandler implements InvocationHandler, Serializable
    {
        private Class<?> c;
        private InetSocketAddress address;

        public StubHandler(Class<?> c, InetSocketAddress address)
        {
            this.c = c;
            this.address = address;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
        {
            if(isRemoteMethod(method))
            {
                return remoteInvocation(proxy, method, args);
            }
            else
            {
                if(method.getName().equals("equals"))
                {
                    return localEquals(proxy, args);
                }
                if (method.getName().equals("hashCode"))
                {
                    return localHashCode(proxy, args);
                }
                if (method.getName().equals("toString"))
                {
                    return localToString(proxy, args);
                }
                throw new RMIException(new NoSuchMethodException());
            }
        }

        private boolean isRemoteMethod(Method method)
        {
            for (Method m : c.getMethods())
            {
                if (m.equals(method))
                {
                    return true;
                }
            }
            return false;
        }

        private boolean localEquals(Object proxy, Object[] args)
        {
            if (args == null)
            {
                return false;
            }

            if (args.length != 1)
            {
                return false;
            }

            if (args[0] == null)
            {
                return false;
            }

            if (!args[0].getClass().equals(proxy.getClass()))
            {
                return false;
            }

            StubHandler stubHandler1 = (StubHandler)Proxy.getInvocationHandler(args[0]);
            StubHandler stubHandler2 = (StubHandler)Proxy.getInvocationHandler(proxy);

            return stubHandler1.address.equals(stubHandler2.address);
        }

        private int localHashCode(Object proxy, Object[] args) throws Throwable
        {
            if (args != null)
            {
                throw new IllegalArgumentException("Method hashCode takes no argument!");
            }

            StubHandler stubHandler = (StubHandler)Proxy.getInvocationHandler(proxy);

            return stubHandler.address.hashCode() + proxy.getClass().hashCode();
        }

        private String localToString(Object proxy, Object[] args) throws Throwable
        {
            if (args != null)
            {
                throw new IllegalArgumentException("Method toString takes no argument!");
            }

            StubHandler stubHandler = (StubHandler)Proxy.getInvocationHandler(proxy);
            String name = "Remote interface: " + proxy.getClass().getInterfaces()[0].toString();
            String addr = "Remote address: " + stubHandler.address.toString();

            return name+'\n'+addr+'\n';
        }

        private Object remoteInvocation(Object proxy, Method method, Object[] args) throws Throwable
        {
            Object remoteObject = null;
            RMIStatus rmiStatus;

            try
            {
                StubHandler stubHandler = (StubHandler) Proxy.getInvocationHandler(proxy);
                Socket clientSocket = new Socket(stubHandler.address.getAddress(), stubHandler.address.getPort());

                ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
                out.flush();

                out.writeObject(method.getName());
                out.writeObject(method.getParameterTypes());
                out.writeObject(args);

                ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());

                rmiStatus = (RMIStatus) in.readObject();
                remoteObject = in.readObject();

                clientSocket.close();

                if (rmiStatus == RMIStatus.OK)
                {
                    return remoteObject;
                }
                else if(rmiStatus == RMIStatus.RMI_EXCEPTION)
                {
                    throw (Throwable) remoteObject;
                }
            }
            catch (Exception e)
            {
                throw new RMIException(e);
            }
            throw (Throwable) remoteObject;
        }
    }
}
