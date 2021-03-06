package bot;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import bot.CommandStructure.*;
import bot.database.manager.DatabaseManager;
import net.dv8tion.jda.bot.entities.ApplicationInfo;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent;
import net.dv8tion.jda.core.events.message.*;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.requests.RestAction;


public class EventListener extends ListenerAdapter {
	DatabaseManager dbMan;
	String botAdmin;
	User botOwner; //We'll hold the botOwner so we don't have to keep asking Discord for this
	Map<String, CommandStructure> cmdList = new HashMap<String,CommandStructure>();
	
	public EventListener(DatabaseManager dbMan) {
		selfStart(dbMan, null);
	}
	
	public EventListener(DatabaseManager dbMan, String botAdmin) {
		selfStart(dbMan, botAdmin);
	}
	
	private void selfStart(DatabaseManager dbMan, String botAdmin)
	{
		this.dbMan = dbMan;
		this.botAdmin = botAdmin;
		
	}
	
	private void setupCommandList(ApplicationInfo info) 
	{
		//TODO I think this could be improved fair better. 
		botOwner = info.getOwner();
		
		String name = "setprefix";
		cmdList.put(name, new SetPrefixCS(dbMan, botAdmin, botOwner, name, 1, 1));
		
		name = "setlevelforuser";
		cmdList.put(name, new SetPermissionByUserCS(dbMan, botAdmin, botOwner, name, 2, 1));
		
		name = "setcommandlevel";
		cmdList.put(name, new SetCommandLevelCS(dbMan, botAdmin, botOwner, name, 3, 1));
		
		name = "profile";
		cmdList.put(name, new ProfileCS(dbMan,botAdmin, botOwner, name, 4, 999));
		
		name = "preload";
		cmdList.put(name, new PreloadCS(dbMan,botAdmin, botOwner, name, 5, 1));
		
		name = "setlevelforrole";
		cmdList.put(name, new SetPermissionsByRoleCS(dbMan,botAdmin, botOwner, name, 6, 1));
		
		name = "help";
		cmdList.put(name, new HelpCS(dbMan,botAdmin, botOwner, name, 7, 999));
		
		name = "iam";
		cmdList.put(name, new SelfRolesCS(dbMan,botAdmin, botOwner, name, 8, 999)); 
		
		name = "selfassignrole";
		cmdList.put(name, new SetSelfRoleCS(dbMan,botAdmin, botOwner, name, 9, 1));
		
		name = "listselfroles";
		cmdList.put(name, new ListSelfRolesCS(dbMan,botAdmin, botOwner, name, 10, 999));
		
		name = "removeselfassignrole";
		cmdList.put(name, new RemoveSelfRoleCS(dbMan,botAdmin, botOwner, name, 11, 1));
		
		name = "setselfassigngroup";
		cmdList.put(name, new SetSelfRoleGroupCS(dbMan,botAdmin, botOwner, name, 12, 1));
		
		name = "togglegroupexculsive";
		cmdList.put(name, new SetSelfRoleGroupExculsive(dbMan,botAdmin, botOwner, name, 13, 1));
		
		name = "removeselfassigngroup";
		cmdList.put(name, new RemoveSelfRoleGroup(dbMan,botAdmin, botOwner, name, 14, 1));
	}

	
	@Override
	public void onReady(ReadyEvent event)
	{
		JDA jda = event.getJDA();
		RestAction<ApplicationInfo> ra = jda.asBot().getApplicationInfo();
		//fetch botOwner;
		Consumer<ApplicationInfo> callback = (info) -> setupCommandList(info);
		ra.queue(callback);
	}
	
	@Override
	public void onGuildJoin(GuildJoinEvent event)
	{
		
		Long guildID = event.getGuild().getIdLong();
		try {
			dbMan.setNewPermissionNames(guildID);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	//We Want To See All Users Joinning the server(Called Guilds by Discord, why, I dunno)
	@Override
	public void onGuildMemberJoin(GuildMemberJoinEvent event)
	{
		Member member = event.getMember();
        Guild guild = event.getGuild();
        //TODO Update this to be adjustable for now, we'll use event-log
        
        List<TextChannel> ltc = guild.getTextChannelsByName("event-log", true);
        if (ltc.isEmpty())
        {
        	System.out.printf("Found no text channels with event-log");
        	return;
        }
        
        for(TextChannel tc : ltc)
        {
        	tc.sendMessage("Member " + member.getAsMention() + " has joined " + guild.getName() + ".").queue();
   		}
	}
	
	//Users leaving server
	@Override
	public void onGuildMemberLeave(GuildMemberLeaveEvent event)
	{
		Member member = event.getMember();
        Guild guild = event.getGuild();
        //TODO Update this to be adjustable for now, we'll use event-log
        List<TextChannel> ltc=  guild.getTextChannelsByName("event-log", true);

        if (ltc.isEmpty())
        {
        	System.out.printf("Found no text channels with event-log");
        	return;
        }
        
        for(TextChannel tc : ltc)
        {
        	tc.sendMessage("Member `" + member.getEffectiveName() + "` has left " + guild.getName() + ".").queue();
   		}
	}

	
	@Override
	public void onGuildMessageReceived(GuildMessageReceivedEvent event)
	{
		Member author = event.getMember(); //User who sent message, member of guild
		Long userID = author.getUser().getIdLong();
		MessageChannel channel = event.getChannel();
		Message message = event.getMessage(); //Message recieved
		String msg = message.getContent(); // String readable content of message
		Guild guild = event.getGuild(); //Get info about the server this message is recieved on
		Long guildID = guild.getIdLong(); //guild unique id
		
		String guildPrefix = "!"; //Command prefix, Default to ! <- break this out?
		
		if(dbMan.getPrefix(guildID) != null)
		{
			guildPrefix = dbMan.getPrefix(guildID);
		}
		
		//Check to make sure our commands are setup (async can be a bitch)
		
		//Check Prefix
		if (msg.length() > 0)
		{
			String msgPrefix = msg.substring(0, guildPrefix.length());
			String msgCommand = msg.substring(guildPrefix.length()).toLowerCase();
	
			if(msgPrefix.equals(guildPrefix)) 
			{
				if(cmdList.isEmpty())
				{
					//Our commands list have not setup yet, we're still waiting for infomation from Discord
					channel.sendMessage("My Command List has not been initiziated yet. Still waiting on infomation from Discord. (If this taking more than a minute, there's something wrong)").queue();
				} else {
					//Loop through our commands
					for(String commandName : cmdList.keySet())
					{
						if(msgCommand.startsWith(commandName)){
							Integer cmdCharCount = guildPrefix.length() + commandName.length();
							String parameters = msg.substring(cmdCharCount);
							
							cmdList.get(commandName).excute(author, channel, message, parameters, cmdList);
							break; //We found a matching command, let break out of the loop
						}
					}
				}
			} else {
				//No prefix found, we'll look for table flip/unflip and inc counts for that user
				if(msg.contains("(╯°□°）╯︵ ┻━┻"))
				{
					try {
						dbMan.incUserFlipped(guildID, userID);
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else if(msg.contains("┬─┬﻿ ノ( ゜-゜ノ)"))
				{
					try {
						dbMan.incUserUnflipped(guildID, userID);
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	/**
     * NOTE THE @Override!
     * This method is actually overriding a method in the ListenerAdapter class! We place an @Override annotation
     *  right before any method that is overriding another to guarantee to ourselves that it is actually overriding
     *  a method from a super class properly. You should do this every time you override a method!
     *
     * As stated above, this method is overriding a hook method in the
     * {@link net.dv8tion.jda.core.hooks.ListenerAdapter ListenerAdapter} class. It has convience methods for all JDA events!
     * Consider looking through the events it offers if you plan to use the ListenerAdapter.
     *
     * In this example, when a message is received it is printed to the console.
     *
     * @param event
     *          An event containing information about a {@link net.dv8tion.jda.core.entities.Message Message} that was
     *          sent in a channel.
     */
	
	//All Messages recieved, from Private channels (DM), Public Channels(server/guild), Groups (Client only, we're using bot account so we can't do groups)
    @Override
    public void onMessageReceived(MessageReceivedEvent event)
    {
        //These are provided with every event in JDA
        //JDA jda = event.getJDA();                       //JDA, the core of the api.
        
        
        //long responseNumber = event.getResponseNumber();//The amount of discord events that JDA has received since the last reconnect.

        //Event specific information
        User author = event.getAuthor();                //The user that sent the message
        Message message = event.getMessage();           //The message that was received.
        
        String msg = message.getContent();              //This returns a human readable version of the Message. Similar to
                                                        // what you would see in the client.
        boolean isBotAdminOwner = isBotAdminOwner(author);
        
        if (msg.equals("!!shutdown"))
        {
        	//Make sure we have permission
        	if(isBotAdminOwner) {
        		author.openPrivateChannel().queue((channel) -> sendChannelMessageAndShutdown(channel, "Bye bye, I'm closing down"));
        	}
        }    
    }

	private boolean isBotAdminOwner(User author) {
		String userwithDiscriminator = author.getName() + "#" + author.getDiscriminator(); //the libarey don't include a readily used readable username with descriminator
		return (userwithDiscriminator.equals(botAdmin) && botAdmin != null) || (botOwner.getIdLong() == author.getIdLong());
	}
	
	    
    public void sendChannelMessageAndShutdown(MessageChannel channel, String message)
    {
    	Consumer<Message> callback = (response) -> response.getJDA().shutdown();
    	channel.sendMessage(message).queue(callback);
    }
    
}
