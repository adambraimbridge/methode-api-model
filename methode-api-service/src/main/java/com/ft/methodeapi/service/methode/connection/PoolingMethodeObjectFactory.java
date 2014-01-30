package com.ft.methodeapi.service.methode.connection;

import EOM.FileSystemAdmin;
import EOM.Repository;
import EOM.Session;
import com.ft.methodeapi.metrics.FTTimer;
import com.ft.methodeapi.metrics.RunningTimer;
import com.ft.methodeapi.service.methode.MethodeException;
import com.google.common.base.Preconditions;
import org.omg.CORBA.ORB;
import org.omg.CORBA.TIMEOUT;
import org.omg.CORBA.TRANSIENT;
import org.omg.CosNaming.NamingContextExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stormpot.Allocator;
import stormpot.Config;
import stormpot.LifecycledResizablePool;
import stormpot.PoolException;
import stormpot.Timeout;
import stormpot.bpool.BlazePool;

import java.util.concurrent.TimeUnit;

/**
 * PoolingMethodeObjectFactory
 *
 * @author Simon.Gibbs
 */
public class PoolingMethodeObjectFactory implements MethodeObjectFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(PoolingMethodeObjectFactory.class);

    private final FTTimer claimConnectionTimer = FTTimer.newTimer(PoolingMethodeObjectFactory.class, "claim-connection");
    private final FTTimer releaseConnectionTimer = FTTimer.newTimer(PoolingMethodeObjectFactory.class, "release-connection");

    ThreadLocal<MethodeConnection> allocatedConnection = new ThreadLocal<MethodeConnection>() {
        @Override
        protected MethodeConnection initialValue() {

            RunningTimer timer = claimConnectionTimer.start();
            try {
                LOGGER.debug("Claiming MethodeConnection");
                return pool.claim(timeout);
            } catch (InterruptedException | PoolException e) {
                throw new MethodeException(e);
            } finally {
                timer.stop();
            }
        }
    };

    private final MethodeObjectFactory implementation;
    private final LifecycledResizablePool<MethodeConnection> pool;
    private final Timeout timeout;


    public PoolingMethodeObjectFactory(final MethodeObjectFactory implementation, int poolSize) {
        Allocator<MethodeConnection> allocator = new MethodeConnectionAllocator(implementation);

        Config<MethodeConnection> config = new Config<MethodeConnection>().setAllocator(allocator);
        config.setSize(poolSize);
        config.setExpiration(new TimeSpreadExpiration(5,10,TimeUnit.MINUTES));


        pool = new SelfCleaningPool<>(new BlazePool<>(config), TIMEOUT.class, TRANSIENT.class);
        timeout = new Timeout(10, TimeUnit.SECONDS);
        this.implementation = implementation;

    }

    @Override
    public ORB createOrb() {
        return allocatedConnection.get().getOrb();  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public NamingContextExt createNamingService(ORB orb) {
        Preconditions.checkState(orb==this.createOrb());
        return allocatedConnection.get().getNamingService();
    }

    @Override
    public Repository createRepository(NamingContextExt namingService) {
        return allocatedConnection.get().getRepository();
    }

    @Override
    public Session createSession(Repository repository) {
        return allocatedConnection.get().getSession();
    }

    @Override
    public FileSystemAdmin createFileSystemAdmin(Session session) {
        return allocatedConnection.get().getFileSystemAdmin();
    }

    @Override
    public void maybeCloseFileSystemAdmin(FileSystemAdmin fileSystemAdmin) {
        // deferred to allocator
    }

    @Override
    public void maybeCloseSession(Session session) {
        // deferred to allocator
    }

    @Override
    public void maybeCloseRepository(Repository repository) {
        // deferred to allocator
    }

    @Override
    public void maybeCloseNamingService(NamingContextExt namingService) {
        // deferred to allocator
    }

    @Override
    public void maybeCloseOrb(ORB orb) {
        RunningTimer timer = releaseConnectionTimer.start();
        try {
            Preconditions.checkState(orb==this.createOrb());
            LOGGER.debug("Releasing MethodeConnection");
            allocatedConnection.get().release();
            allocatedConnection.remove();
        } finally {
            timer.stop();
        }
    }

    @Override
    public String getDescription() {
        return String.format("[%d x [%s]]",pool.getTargetSize(),implementation.getDescription());
    }
}
