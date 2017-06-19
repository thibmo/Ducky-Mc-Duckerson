package bot.CommandStructure;

import java.util.List;
import java.util.Map;

import bot.database.manager.DatabaseManager;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.User;
import java.awt.Color;

public class ProfileCS extends CommandStructure {
	
	public ProfileCS(DatabaseManager dbMan, String botAdmin, User botOwner, String commandName, int commandID,
			int commandDefaultLevel) {
		super(dbMan, botAdmin, botOwner, commandName, commandID, commandDefaultLevel);
	}

	@Override
	public void excute(Member author, MessageChannel channel, Message message, String parameters,
			Map<String, CommandStructure> commandList) {
		Long guildID = author.getGuild().getIdLong();
		String prefix = dbMan.getPrefix(guildID);
		 
		
		
		List<User> mentionedUsers = message.getMentionedUsers();
		if(hasPermission(author))
		{
			if(mentionedUsers.isEmpty())
			{
				Integer userLevel = getPermissionLevel(author);
				String userLevelName = dbMan.getLevelName(guildID, userLevel);
				Color color = new Color(255, 40, 40);
				sendProfile(author, channel, prefix, userLevelName, color);
			} else {
				//We can't loop through the whole list of mentioned users, so we only going to grab the first mentioned user and ignored the rest
				User user = mentionedUsers.get(0); 
				Color color = new Color(100, 40, 240);
				Member userMember = author.getGuild().getMemberById(user.getIdLong());
				
				if (userMember == null) {
					channel.sendMessage("I cannot find " + user.getAsMention() + " in this guild. I can only show profiles that are on this server.").queue();
				} else {
					Integer userLevel = getPermissionLevel(userMember);
					String userLevelName = dbMan.getLevelName(guildID, userLevel);
					sendProfile(userMember, channel, prefix, userLevelName, color);
				}
			}
		}
	}

	private void sendProfile(Member member, MessageChannel channel, String prefix, String userLevelName, Color color) {
		EmbedBuilder embed = new EmbedBuilder();
		
		embed.setColor(color);
		embed.setAuthor("Profile of " + member.getEffectiveName(), null, member.getUser().getAvatarUrl());
		embed.addField("Rank:", "In Development", true);
		embed.addField("Level:", "0", true);
		embed.addField("Points:", "1", true);
		embed.addField("Balance:" , "-1 " + ":moneybag:", true);
		embed.setFooter("To see your profile, use " + prefix + "profile", null);
		embed.setThumbnail(member.getUser().getAvatarUrl());
		embed.setDescription(userLevelName);
		channel.sendMessage(embed.build()).queue();
	}

}