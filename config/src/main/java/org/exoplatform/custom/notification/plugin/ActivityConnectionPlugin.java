/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.custom.notification.plugin;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.MessageInfo;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.plugin.AbstractNotificationPlugin;
import org.exoplatform.commons.api.notification.service.template.TemplateContext;
import org.exoplatform.commons.notification.template.TemplateUtils;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.manager.RelationshipManager;
import org.exoplatform.social.notification.LinkProviderUtils;
import org.exoplatform.social.notification.Utils;
import org.exoplatform.social.notification.plugin.SocialNotificationUtils;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ActivityConnectionPlugin extends AbstractNotificationPlugin {
  public static final String ID = "ActivityConnectionPlugin";
  
  public ActivityConnectionPlugin(InitParams initParams) {
    super(initParams);
  }

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public NotificationInfo makeNotification(NotificationContext ctx) {
    try {
      ExoSocialActivity activity = ctx.value(SocialNotificationUtils.ACTIVITY);
      IdentityManager identityManager = Utils.getIdentityManager();
      Identity posterIdentity = identityManager.getIdentity(activity.getPosterId(), false);
      return NotificationInfo.instance()
          .to(getConnections(posterIdentity))
          .with(SocialNotificationUtils.POSTER.getKey(), posterIdentity.getRemoteId())
          .with(SocialNotificationUtils.ACTIVITY_ID.getKey(), activity.getId())
          .key(getId()).end();
      
    } catch (Exception e) {
      ctx.setException(e);
    }
    
    return null;
  }

  @Override
  public MessageInfo makeMessage(NotificationContext ctx) {
	  MessageInfo messageInfo = new MessageInfo();
	    
	    NotificationInfo notification = ctx.getNotificationInfo();
	    
	    String language = getLanguage(notification);
	    TemplateContext templateContext = new TemplateContext(notification.getKey().getId(), language);
	    SocialNotificationUtils.addFooterAndFirstName(notification.getTo(), templateContext);

	    String activityId = notification.getValueOwnerParameter(SocialNotificationUtils.ACTIVITY_ID.getKey());
	    ExoSocialActivity activity = Utils.getActivityManager().getActivity(activityId);
	    Identity identity = Utils.getIdentityManager().getIdentity(activity.getPosterId(), true);
	    
	    
	    templateContext.put("USER", identity.getProfile().getFullName());
	    String subject = TemplateUtils.processSubject(templateContext);
	    
	    templateContext.put("PROFILE_URL", LinkProviderUtils.getRedirectUrl("user", identity.getRemoteId()));
	    templateContext.put("ACTIVITY", activity.getTitle());
	    templateContext.put("REPLY_ACTION_URL", LinkProviderUtils.getRedirectUrl("reply_activity", activity.getId()));
	    templateContext.put("VIEW_FULL_DISCUSSION_ACTION_URL", LinkProviderUtils.getRedirectUrl("view_full_activity", activity.getId()));
	    String body = TemplateUtils.processGroovy(templateContext);
	    
	    return messageInfo.subject(subject).body(body).end();
  }

  @Override
  public boolean makeDigest(NotificationContext ctx, Writer writer) {
    List<NotificationInfo> notifications = ctx.getNotificationInfos();
    NotificationInfo first = notifications.get(0);
    String sendToUser = first.getTo();
    String language = getLanguage(first);
    
    TemplateContext templateContext = new TemplateContext(first.getKey().getId(), language);
    Map<String, List<String>> receiverMap = new LinkedHashMap<String, List<String>>();
    
    try {
      for (NotificationInfo message : notifications) {
        ExoSocialActivity activity = Utils.getActivityManager().getActivity(message.getValueOwnerParameter(SocialNotificationUtils.ACTIVITY_ID.getKey()));
        
        //Case of activity was deleted, ignore this notification
        if (activity == null) {
          continue;
        }
        SocialNotificationUtils.processInforSendTo(receiverMap, sendToUser, message.getValueOwnerParameter(SocialNotificationUtils.POSTER.getKey()));
      }
      writer.append(SocialNotificationUtils.getMessageByIds(receiverMap, templateContext, "user"));
    } catch (IOException e) {
      ctx.setException(e);
      return false;
    }
    return true;
  }

  @Override
  public boolean isValid(NotificationContext ctx) {
	ExoSocialActivity activity = ctx.value(SocialNotificationUtils.ACTIVITY);
	if (activity.getStreamOwner().equals(Utils.getUserId(activity.getPosterId()))) {
	  return true;
	}

	return false;
  }
  
  public List<String> getConnections(Identity posterIdentity) throws Exception {
	  List<String> connections = new ArrayList<String>(); 
	  RelationshipManager re = (RelationshipManager) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(RelationshipManager.class);
	  
	  ListAccess<Identity> listAccess = re.getConnections(posterIdentity);
	  
	  int offset = 0, limit = 45, size = listAccess.getSize();

	  while(offset < size) {
		  Identity[] identities = listAccess.load(offset, limit);
		  if(identities.length == 0) { break; }
		  for (int i = 0; i < identities.length; ++i) {
			  connections.add(identities[i].getRemoteId());
		  }
		  offset += limit;
	  }
	  
	  return connections;   
  }
}
