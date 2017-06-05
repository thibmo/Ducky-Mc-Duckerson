package bot;
/*
 *     Copyright 2015-2017 Austin Keener & Michael Ritter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import net.dv8tion.jda.client.entities.Group;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import javax.security.auth.login.LoginException;
import java.util.List;
import java.util.Random;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;


//TODO Logging (to echo certain logs that's not tracked in Aduit. Such as quit/joins.
//TODO Permissions, set custom permissions, tie into role, so for example someone with the role "BotHandler" 
//		can do everything while someone with indepentent role can do games only. 
//TODO Welcome after someone select .iam for the first time
//TODO Role assistment 
//TODO Greeting Message - Greet someone joinning for the first time  
//TODO Custom Reaction - !fb/!faq and so on, allow this to be defined at runtime instead of hard code
//TODO FAQ - allow someone to request a FAQ link to Guns of Icarus Online   
//TODO Forum Tie in - Scrape the forum of user profiles. (This is just about the only thing we can do, without hacking the game client.  <- We might do this.
//TODO Music - The way Discord handles Bots, is that we can have different programs use the same token so we can easily have another bot running just for 
//		Music and not worry about coding it into this program

public class McDucky
{
	//Contains private keys, for bot Tokens, Database and other configs
	private File configFile = new File("config.properties");
	private Properties configProps;
	
	private void loadProperties() throws IOException {
		configProps = new Properties();
		
		// loads properties from file
		InputStream inputStream = new FileInputStream(configFile);
		configProps.load(inputStream);
		inputStream.close();
	}
	
	private void setupProperties() throws FileNotFoundException, IOException
	{
		//List all the config we need to successful run this bot
		configProps.setProperty("bot_token", "");
		configProps.setProperty("debug_level", "0"); //Set to Off
		
		//Save to file		
		OutputStream outputStream = new FileOutputStream(configFile);
		configProps.store(outputStream, "Set your configs here");
		outputStream.close();
	}
	
	//Our setup. My god, Abby is terrible at commenting
	public McDucky() {

    	//Load properties from file
    	try
    	{
    		loadProperties();
    	} catch (IOException ex) {
    		System.out.printf("there's no config file, creating file.");
			try {
				setupProperties();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			//Alert the user that, hey, we need values to run. 
			System.out.printf("Set values in config.propteries before running bot.");
			return;
    		
    	}
    	
    		//Load token
    		String botToken = configProps.getProperty("bot_token");
    		int debugLevel;
    		try {
    			 debugLevel = Integer.parseInt(configProps.getProperty("debug_level"));
    		} catch (NumberFormatException e)
    		{
    			System.out.printf("debug_level is not a valid number. switching to debug off");
    			debugLevel = 0;
    		}
    		DebugLevel db = DebugLevel.getDebugLevel(debugLevel);
    		
    		if (botToken.isEmpty())
    		{
    			//We GOT to have this value
    			System.out.printf("Bot Token Cannot be empty. Set bot token in the config.properties");
    			return;
    		}
    		
        //We construct a builder for a BOT account. If we wanted to use a CLIENT account
        // we would use AccountType.CLIENT
        try
        {
            JDA jda = new JDABuilder(AccountType.BOT)
                .setToken(botToken)  //The token of the account that is logging in.
                .addEventListener(new debugListener(db)) //An instance of a class to handle verbose event logging
                .addEventListener(new eventListener())  //An instance of a class that will handle events.
                .buildBlocking();  //There are 2 ways to login, blocking vs async. Blocking guarantees that JDA will be completely loaded.
        }
        catch (LoginException e)
        {
            //If anything goes wrong in terms of authentication, this is the exception that will represent it
            e.printStackTrace();
        }
        catch (InterruptedException e)
        {
            //Due to the fact that buildBlocking is a blocking method, one which waits until JDA is fully loaded,
            // the waiting can be interrupted. This is the exception that would fire in that situation.
            //As a note: in this extremely simplified example this will never occur. In fact, this will never occur unless
            // you use buildBlocking in a thread that has the possibility of being interrupted (async thread usage and interrupts)
            e.printStackTrace();
        }
        catch (RateLimitedException e)
        {
            //The login process is one which can be ratelimited. If you attempt to login in multiple times, in rapid succession
            // (multiple times a second), you would hit the ratelimit, and would see this exception.
            //As a note: It is highly unlikely that you will ever see the exception here due to how infrequent login is.
            e.printStackTrace();
        }
	}
	
    /**
     * This is the method where the program starts.
     */
    public static void main(String[] args)
    {
    	new McDucky();
    }

    
}