package net.md_5.bungee.api.plugin;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.io.File;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import lombok.Getter;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ConfigurationAdapter;
import net.md_5.bungee.api.scheduler.GroupedThreadFactory;
import net.md_5.bungee.api.scheduler.RxBungeeScheduler;
import net.md_5.bungee.event.EventExecutor;
import net.md_5.bungee.event.EventPriority;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

/**
 * Represents any Plugin that may be loaded at runtime to enhance existing
 * functionality.
 */
public class Plugin
{

    @Getter
    private PluginDescription description;
    @Getter
    private ProxyServer proxy;
    @Getter
    private File file;
    @Getter
    private Logger logger;
    @Getter
    private CompositeSubscription subscribedEventHandlers = new CompositeSubscription();
    @Getter
    private RxBungeeScheduler rxScheduler = new RxBungeeScheduler(this);

    /**
     * Called when the plugin has just been loaded. Most of the proxy will not
     * be initialized, so only use it for registering
     * {@link ConfigurationAdapter}'s and other predefined behavior.
     */
    public void onLoad()
    {
    }

    /**
     * Called when this plugin is enabled.
     */
    public void onEnable()
    {
    }

    /**
     * Called when this plugin is disabled.
     */
    public void onDisable()
    {
    }

    /**
     * Gets the data folder where this plugin may store arbitrary data. It will
     * be a child of {@link ProxyServer#getPluginsFolder()}.
     *
     * @return the data folder of this plugin
     */
    public final File getDataFolder()
    {
        return new File( getProxy().getPluginsFolder(), getDescription().getName() );
    }

    /**
     * Get a resource from within this plugins jar or container. Care must be
     * taken to close the returned stream.
     *
     * @param name the full path name of this resource
     * @return the stream for getting this resource, or null if it does not
     * exist
     */
    public final InputStream getResourceAsStream(String name)
    {
        return getClass().getClassLoader().getResourceAsStream( name );
    }

    /**
     * Called by the loader to initialize the fields in this plugin.
     *
     * @param proxy The ProxyServer
     * @param description the description that describes this plugin
     */
    final void init(ProxyServer proxy, PluginDescription description)
    {
        this.proxy = proxy;
        this.description = description;
        this.file = description.getFile();
        this.logger = new PluginLogger( this );
    }

    final <T extends Event> Observable<T> observeEvent(final Class<? extends T>... classes) {
        return Observable.create(new Observable.OnSubscribe<T>() {
            @Override
            public void call(final Subscriber<? super T> subscriber) {
                final EventExecutor eventExecutor = new EventExecutor() {
                    @SuppressWarnings("unchecked")
                    @Override
                    public void handleEvent(Object event) {
                        Class<?> eventClass = event.getClass();
                        boolean applies = false;
                        for (Class<? extends T> classMember : classes) {
                            if (classMember.isAssignableFrom(eventClass)) {
                                applies = true;
                                break;
                            }
                        }

                        if (!applies)
                            return;

                        subscriber.onNext((T) event);
                    }
                };

                proxy.getPluginManager().registerExecutor(Plugin.this, eventExecutor);
                subscriber.onStart();
                subscribedEventHandlers.add(subscriber);
                subscriber.add(Subscriptions.create(new Action0() {
                    @Override
                    public void call() {
                        subscribedEventHandlers.remove(subscriber);
                        proxy.getPluginManager().unregisterExecutor(eventExecutor);
                    }
                }));
            }
        }).observeOn(rxScheduler);
    }

    //
    private ExecutorService service;

    @Deprecated
    public ExecutorService getExecutorService()
    {
        if ( service == null )
        {
            String name = ( getDescription() == null ) ? "unknown" : getDescription().getName();
            service = Executors.newCachedThreadPool( new ThreadFactoryBuilder().setNameFormat( name + " Pool Thread #%1$d" )
                    .setThreadFactory( new GroupedThreadFactory( this, name ) ).build() );
        }
        return service;
    }
    //
}
