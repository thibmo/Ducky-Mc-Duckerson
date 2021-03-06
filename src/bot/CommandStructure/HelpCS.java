package bot.CommandStructure;

import java.util.Map;

import bot.database.manager.DatabaseManager;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.PrivateChannel;
import net.dv8tion.jda.core.entities.User;

public class HelpCS extends CommandStructure {

	public HelpCS(DatabaseManager dbMan, String botAdmin, User botOwner, String commandName, int commandID,
			int commandDefaultLevel) {
		super(dbMan, botAdmin, botOwner, commandName, commandID, commandDefaultLevel);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void excute(Member author, MessageChannel channel, Message message, String parameters,
			Map<String, CommandStructure> commandList) {
		
		Long guildID = author.getGuild().getIdLong();
		User user = author.getUser();
		
		user.openPrivateChannel().queue((privChannel) -> sendHelpList(privChannel, commandList, guildID));
			
	}
private void sendHelpList(PrivateChannel privChannel, Map<String, CommandStructure> commandList, Long guildID) {
			EmbedBuilder embed = new EmbedBuilder();
			int count = 0;
			for(String commandName : commandList.keySet())
			{
				String help = commandList.get(commandName).help(guildID);
				if(help == null) help = "";
				embed.addField(commandName, help, true);
				count++;
				if(count > 7)
				{
					count=0;
					privChannel.sendMessage(embed.build()).queue();
					embed = new EmbedBuilder();
				}
			}
			
			privChannel.sendMessage(embed.build()).queue();
	}

	
	@Override
	public String help(Long guildID) {
		
		return "returns a list of commands";
		// TODO Auto-generated method stub

	}

}
