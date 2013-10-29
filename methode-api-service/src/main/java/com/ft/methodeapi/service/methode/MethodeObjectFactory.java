package com.ft.methodeapi.service.methode;

import EOM.ObjectLocked;
import EOM.PermissionDenied;
import EOM.Repository;
import EOM.RepositoryError;
import EOM.RepositoryHelper;
import EOM.RepositoryPackage.InvalidLogin;
import EOM.Session;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.MetricsRegistry;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.core.TimerContext;
import org.omg.CORBA.ORB;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;
import org.omg.CosNaming.NamingContextPackage.CannotProceed;
import org.omg.CosNaming.NamingContextPackage.NotFound;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * Encapsulates logic to create and destroy Methode objects and holds the connection and credential details.
 *
 * @author Simon.Gibbs
 */
public class MethodeObjectFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodeObjectFactory.class);

    private final String username;
    private final String password;
    private final String orbClass;
    private final String orbInitRef;

    private final String orbSingletonClass;

    private final MetricsRegistry metricsRegistry = Metrics.defaultRegistry();

    private final Timer createSessionTimer = metricsRegistry.newTimer(MethodeObjectFactory.class, "create-methode-session");
    private final Timer closeSessionTimer = metricsRegistry.newTimer(MethodeObjectFactory.class, "close-methode-session");

    private final Timer createNamingServiceTimer = metricsRegistry.newTimer(MethodeObjectFactory.class, "create-naming-service");
    private final Timer closeNameServiceTimer = metricsRegistry.newTimer(MethodeObjectFactory.class, "close-naming-service");

    private final Timer createOrbTimer = metricsRegistry.newTimer(MethodeObjectFactory.class, "create-orb");
    private final Timer closeOrbTimer = metricsRegistry.newTimer(MethodeObjectFactory.class, "close-orb");

    private final Timer createRepositoryTimer = metricsRegistry.newTimer(MethodeObjectFactory.class, "create-repository");
    private final Timer closeRepositoryTimer = metricsRegistry.newTimer(MethodeObjectFactory.class, "close-repository");

    public MethodeObjectFactory(String hostname, int port, String username, String password, String orbClass, String orbSingletonClass) {
        this.username = username;
        this.password = password;
        this.orbClass = orbClass;
        this.orbSingletonClass = orbSingletonClass;
        orbInitRef = String.format("NS=corbaloc:iiop:%s:%d/NameService", hostname, port);    }

    public MethodeObjectFactory(Builder builder) {
        this(builder.host, builder.port, builder.username, builder.password, builder.orbClass, builder.orbSingletonClass);
    }

    public Session createSession(Repository repository) {
        Session session;
        final TimerContext timerContext = createSessionTimer.time();
        try {
            session = repository.login(username, password, "", null);
        } catch (InvalidLogin | RepositoryError e) {
            throw new MethodeException(e);
        } finally {
            timerContext.stop();
        }
        return session;
    }

    public NamingContextExt createNamingService(ORB orb) {
        final TimerContext timerContext = createNamingServiceTimer.time();
        try {
            return NamingContextExtHelper.narrow(orb.resolve_initial_references("NS"));
        } catch (InvalidName invalidName) {
            throw new MethodeException(invalidName);
        } finally {
            timerContext.stop();
        }
    }

    public void maybeCloseNamingService(NamingContextExt namingService) {
        if (namingService != null) {
            final TimerContext timerContext = closeNameServiceTimer.time();
            try {
                namingService._release();
            } finally {
                timerContext.stop();
            }
        }
    }

    public Repository createRepository(NamingContextExt namingService) {
        final TimerContext timerContext = createRepositoryTimer.time();
        try {
            return RepositoryHelper.narrow(namingService.resolve_str("EOM/Repositories/cms"));
        } catch (org.omg.CosNaming.NamingContextPackage.InvalidName
                | CannotProceed | NotFound e) {
            throw new MethodeException(e);
        } finally {
            timerContext.stop();
        }
    }

    public ORB createOrb() {
        final TimerContext timerContext = createOrbTimer.time();
        try {
            String[] orbInits = {"-ORBInitRef", orbInitRef};
            Properties properties = new Properties() {
                {
                    setProperty("org.omg.CORBA.ORBClass", orbClass);
                    setProperty("org.omg.CORBA.ORBSingletonClass", orbSingletonClass);
                }
            };
            return ORB.init(orbInits, properties);
        } finally {
            timerContext.stop();
        }
    }

    public void maybeCloseSession(Session session) {
        if (session != null) {
            final TimerContext timerContext = closeSessionTimer.time();
            try {
                session.destroy();
                session._release();
            } catch (PermissionDenied | ObjectLocked | RepositoryError e) {
                LOGGER.warn("failed to destroy EOM.Session", e);
            } finally {
                timerContext.stop();
            }
        }
    }

    public void maybeCloseOrb(ORB orb) {
        if (orb != null) {
            final TimerContext timerContext = closeOrbTimer.time();
            try {
                orb.destroy();
            } finally {
                timerContext.stop();
            }
        }
    }

    public void maybeCloseRepository(Repository repository) {
        if(repository!=null) {
            final TimerContext timerContext = closeRepositoryTimer.time();
            try {
                repository._release();
            } finally {
                timerContext.stop();
            }
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String username;
        private String password;
        private String host;
        private int port;
        private String orbClass;
        private String orbSingletonClass;

        public Builder withUsername(String username) {
            this.username = username;
            return this;
        }

        public Builder withPassword(String password) {
            this.password = password;
            return this;
        }

        public Builder withHost(String host) {
            this.host = host;
            return this;
        }

        public Builder withPort(int port) {
            this.port = port;
            return this;
        }

        public Builder withOrbClass(String orbClass) {
            this.orbClass = orbClass;
            return this;
        }

        public Builder withOrbSingletonClass(String orbSingletonClass) {
            this.orbSingletonClass = orbSingletonClass;
            return this;
        }

        public MethodeObjectFactory build() {
            return new MethodeObjectFactory(this);
        }
    }


}
