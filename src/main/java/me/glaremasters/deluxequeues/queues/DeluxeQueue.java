package me.glaremasters.deluxequeues.queues;

import ch.jalu.configme.SettingsManager;
import co.aikar.commands.ACFVelocityUtil;
import co.aikar.commands.MessageType;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.util.MessagePosition;
import com.velocitypowered.api.util.title.TextTitle;
import me.glaremasters.deluxequeues.DeluxeQueues;
import me.glaremasters.deluxequeues.configuration.sections.ConfigOptions;
import me.glaremasters.deluxequeues.events.PlayerQueueEvent;
import me.glaremasters.deluxequeues.messages.Messages;
import me.glaremasters.deluxequeues.tasks.QueueMoveTask;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;

import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

/**
 * Created by Glare
 * Date: 7/13/2019
 * Time: 10:30 PM
 */
public class DeluxeQueue {

    private DeluxeQueues deluxeQueues;
    private LinkedList<Player> queue = new LinkedList<>();
    private RegisteredServer server;
    private int delayLength;
    private int playersRequired;
    private int maxSlots;
    private SettingsManager settingsManager;
    private String notifyMethod;

    public DeluxeQueue(DeluxeQueues deluxeQueues, RegisteredServer server, int playersRequired, int maxSlots) {
        this.deluxeQueues = deluxeQueues;
        this.server = server;
        this.settingsManager = deluxeQueues.getSettingsHandler().getSettingsManager();
        this.delayLength = settingsManager.getProperty(ConfigOptions.DELAY_LENGTH);
        this.playersRequired = playersRequired;
        this.maxSlots = maxSlots;
        this.notifyMethod = settingsManager.getProperty(ConfigOptions.INFORM_METHOD);
        deluxeQueues.getProxyServer().getScheduler().buildTask(deluxeQueues, new QueueMoveTask(this, server))
                .repeat(delayLength, TimeUnit.SECONDS).schedule();
    }

    /**
     * Add a player to a queue
     * @param player the player to add
     */
    public void addPlayer(Player player) {
        if (!queue.contains(player)) {
            deluxeQueues.getProxyServer().getEventManager().fire(new PlayerQueueEvent(player, server))
                    .thenAcceptAsync(result -> {
                        //Don't add to queue if event cancelled, show player the reason
                        if (result.isCancelled()) {
                            deluxeQueues.getCommandManager().sendMessage(player, MessageType.ERROR,
                                                                         Messages.QUEUES__CANNOT_JOIN);
                            player.sendMessage(TextComponent.of(result.getReason()).color(TextColor.RED));
                        } else {
                            queue.add(player);
                            notifyPlayer(player);
                        }
                    });
        }
    }

    /**
     * Add in a check to make sure the player can be added to the queue
     * @return added or not
     */
    public boolean canAddPlayer() {
        return server.getPlayersConnected().size() >= playersRequired;
    }

    /**
     * Get the position of a player in a queue
     * @param player the player to check
     * @return their position
     */
    public int getQueuePos(Player player) {
        return queue.indexOf(player);
    }

    /**
     * Notify the player that they are in the queue
     * @param player the player to check
     */
    public void notifyPlayer(Player player) {

        String actionbar = settingsManager.getProperty(ConfigOptions.ACTIONBAR_DESIGN);
        String message = settingsManager.getProperty(ConfigOptions.TEXT_DESIGN);
        String title_top = settingsManager.getProperty(ConfigOptions.TITLE_HEADER);
        String title_bottom = settingsManager.getProperty(ConfigOptions.TITLE_FOOTER);

        switch (notifyMethod.toLowerCase()) {
            case "actionbar":
                actionbar = actionbar.replace("{server}", server.getServerInfo().getName());
                actionbar = actionbar.replace("{pos}", String.valueOf(getQueuePos(player) + 1));
                actionbar = actionbar.replace("{total}", String.valueOf(queue.size()));
                player.sendMessage(ACFVelocityUtil.color(actionbar), MessagePosition.ACTION_BAR);
                break;
            case "text":
                message = message.replace("{server}", server.getServerInfo().getName());
                message = message.replace("{pos}", String.valueOf(getQueuePos(player) + 1));
                message = message.replace("{total}", String.valueOf(queue.size()));
                player.sendMessage(ACFVelocityUtil.color(message), MessagePosition.SYSTEM);
                break;
            case "title":
                TextTitle.Builder title = TextTitle.builder();
                title.title(ACFVelocityUtil.color(title_top));
                title_bottom = title_bottom.replace("{server}", server.getServerInfo().getName());
                title_bottom = title_bottom.replace("{pos}", String.valueOf(getQueuePos(player) + 1));
                title_bottom = title_bottom.replace("{total}", String.valueOf(queue.size()));
                title.subtitle(ACFVelocityUtil.color(title_bottom));
                player.sendTitle(title.build());
                break;
        }
    }

    /**
     * Check if the queue is holding a player
     * @param player the player to check for
     * @return in the queue or not
     */
    public int checkForPlayer(Player player) {
        return queue.indexOf(player);
    }

    public DeluxeQueues getDeluxeQueues() {
        return this.deluxeQueues;
    }

    public LinkedList<Player> getQueue() {
        return this.queue;
    }

    public RegisteredServer getServer() {
        return this.server;
    }

    public int getDelayLength() {
        return this.delayLength;
    }

    public int getPlayersRequired() {
        return this.playersRequired;
    }

    public int getMaxSlots() {
        return this.maxSlots;
    }

    public SettingsManager getSettingsManager() {
        return this.settingsManager;
    }

    public String getNotifyMethod() {
        return this.notifyMethod;
    }

    public String toString() {
        return "DeluxeQueue(deluxeQueues=" + this.getDeluxeQueues() + ", queue=" + this.getQueue() + ", server=" + this.getServer() + ", delayLength=" + this.getDelayLength() + ", playersRequired=" + this.getPlayersRequired() + ", maxSlots=" + this.getMaxSlots() + ", settingsManager=" + this.getSettingsManager() + ", notifyMethod=" + this.getNotifyMethod() + ")";
    }
}
