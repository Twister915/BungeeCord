package net.md_5.bungee.api.scheduler;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

import java.util.concurrent.TimeUnit;

@EqualsAndHashCode(callSuper = false)
@Data
public final class RxBungeeScheduler extends Scheduler {
    private final Plugin plugin;

    @Override
    public Worker createWorker() {
        return new RxBungeeWorker();
    }

    public class RxBungeeWorker extends Worker {
        private final CompositeSubscription subscription = new CompositeSubscription();

        @Override
        public Subscription schedule(Action0 action) {
            return schedule(action, 0, TimeUnit.MILLISECONDS);
        }

        @Override
        public Subscription schedule(final Action0 action, long delayTime, TimeUnit unit) {
            final ScheduledTask schedule = ProxyServer.getInstance().getScheduler().schedule(plugin, new Runnable() {
                @Override
                public void run() {
                    action.call();
                }
            }, delayTime, unit);

            Subscription subscription = Subscriptions.create(new Action0() {
                @Override
                public void call() {
                    schedule.cancel();
                }
            });

            this.subscription.add(subscription);
            return subscription;
        }

        @Override
        public void unsubscribe() {
            subscription.unsubscribe();
        }

        @Override
        public boolean isUnsubscribed() {
            return subscription.isUnsubscribed();
        }
    }
}
