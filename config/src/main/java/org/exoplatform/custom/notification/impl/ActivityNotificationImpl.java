/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
* modify it under the terms of the GNU Affero General Public License
* as published by the Free Software Foundation; either version 3
* of the License, or (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.custom.notification.impl;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.NotificationKey;
import org.exoplatform.commons.notification.impl.NotificationContextImpl;
import org.exoplatform.custom.notification.plugin.ActivityConnectionPlugin;
import org.exoplatform.social.core.activity.ActivityLifeCycleEvent;
import org.exoplatform.social.core.activity.ActivityListenerPlugin;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.notification.plugin.SocialNotificationUtils;

public class ActivityNotificationImpl extends ActivityListenerPlugin {

  @Override
  public void saveActivity(ActivityLifeCycleEvent event) {

	  ExoSocialActivity activity = event.getSource();    
    NotificationContext ctx = NotificationContextImpl.cloneInstance().append(SocialNotificationUtils.ACTIVITY, activity);

    ctx.getNotificationExecutor().with(ctx.makeCommand(NotificationKey.key(ActivityConnectionPlugin.ID)))
                                 .execute(ctx);
  }

  @Override
  public void updateActivity(ActivityLifeCycleEvent event) {
  }

  @Override
  public void saveComment(ActivityLifeCycleEvent event) { }

  @Override
  public void likeActivity(ActivityLifeCycleEvent event) { }
}