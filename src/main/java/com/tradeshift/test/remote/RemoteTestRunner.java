package com.tradeshift.test.remote;

import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.util.concurrent.CountDownLatch;

import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.Filterable;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runner.manipulation.Sortable;
import org.junit.runner.manipulation.Sorter;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tradeshift.test.remote.internal.InternalRemoteRunner;
import com.tradeshift.test.remote.internal.Utils;

public class RemoteTestRunner extends Runner implements Filterable, Sortable {
    
    private static final Logger log = LoggerFactory.getLogger(RemoteTestRunner.class);

    private Runner delegate;
    private static CountDownLatch serverCountdown = new CountDownLatch(1);

    public RemoteTestRunner(Class<?> clazz) throws InitializationError {
        this(clazz, null, null);
    }

    public RemoteTestRunner(Class<?> clazz, Class<? extends Runner> remoteRunnerClass, String endpoint) throws InitializationError {
        if (remoteRunnerClass == null) {
            Remote remote = Utils.findAnnotation(clazz, Remote.class);
            if (remote != null) {
                endpoint = remote.endpoint();
                remoteRunnerClass = remote.runnerClass();
            } else {
                endpoint = "http://localhost:4578/";
                remoteRunnerClass = BlockJUnit4ClassRunner.class;
            }
        }

        log.info("Trying remote server {} with runner {}", endpoint, remoteRunnerClass.getName());
        if (isAnyRemoteUp(endpoint)) {
            delegate = new InternalRemoteRunner(clazz, endpoint, remoteRunnerClass);
        } else if (isJrebelActive()) {
            startServer(endpoint);
            delegate = new InternalRemoteRunner(clazz, endpoint, remoteRunnerClass);
            try {
                serverCountdown.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
        	delegate = Utils.createRunner(remoteRunnerClass, clazz);
        }
    }

    private void startServer(String endpoint) {
        Thread thread = new Thread() {
            @Override
            public void run() {
                String[] foo = new String[0];
                try {
                    RemoteServer.main(foo);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
        thread.setDaemon(true);
        thread.start();
        //throw new RuntimeException("TODO: finish me");
    }

    private boolean isJrebelActive() {
        try {
             Class.forName("org.zeroturnaround.javarebel.ClassEventListener");
        } catch (ClassNotFoundException e) {
            return false;
        }
        return true;
    }

    private boolean isAnyRemoteUp(String eps) {
    	for (String ep : eps.split(",")) {
    		URI uri = URI.create(ep.trim());
    		
    		try {
    			Socket s = new Socket(uri.getHost(), uri.getPort());
    			s.close();
    			
    			return true;
    		} catch (IOException e) {
    		}
		}

    	return false;
    }
    
    

    @Override
    public Description getDescription() {
        return delegate.getDescription();
    }

    @Override
    public void run(RunNotifier notifier) {
        delegate.run(notifier);
    }
    
    @Override
    public void filter(Filter filter) throws NoTestsRemainException {
        Utils.filter(delegate, filter);
    }
    
    @Override
    public void sort(Sorter sorter) {
        Utils.sort(delegate, sorter);
    }
    

}
