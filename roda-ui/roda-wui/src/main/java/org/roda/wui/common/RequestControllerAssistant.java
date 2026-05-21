/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.wui.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import org.roda.core.data.exceptions.AuthorizationDeniedException;
import org.roda.core.data.exceptions.GenericException;
import org.roda.core.data.exceptions.RequestNotValidException;
import org.roda.core.data.v2.index.IsIndexed;
import org.roda.core.data.v2.index.select.SelectedItems;
import org.roda.core.data.v2.log.LogEntryState;
import org.roda.core.data.v2.user.User;
import org.roda.core.model.utils.UserUtility;

/**
 * @author Gabriel Barros <gbarros@keep.pt>
 */
public class RequestControllerAssistant extends ControllerAssistant {
  private final Object requester;
  private String relatedObjectId;
  private String relatedAipId;
  private Object[] parameters;

  public RequestControllerAssistant(Object requester) {
    this.startDate = new Date();
    this.enclosingMethod = requester.getClass().getEnclosingMethod();
    this.requester = requester;
  }

  // UNAUTHORIZED is logged once per request by RequestHandler.finally with the
  // controller's current relatedAipId/parameters — no need to log here too.

  @Override
  public void checkGroup(User user, String group) throws AuthorizationDeniedException {
    UserUtility.checkGroup(user, group);
  }

  @Override
  public void checkRoles(User user) throws AuthorizationDeniedException {
    UserUtility.checkRoles(user, requester.getClass());
  }

  @Override
  public void checkRoles(User user, Class<?> classToReturn) throws AuthorizationDeniedException {
    UserUtility.checkRoles(user, requester.getClass(), classToReturn);
  }

  @Override
  public <T extends IsIndexed> void checkObjectPermissions(User user, T obj) throws AuthorizationDeniedException {
    checkObjectPermissions(user, obj, null);
  }

  @Override
  public <T extends IsIndexed> void checkObjectPermissions(User user, T obj, Class<?> classToReturn)
    throws AuthorizationDeniedException {
    UserUtility.checkObjectPermissions(user, obj, requester.getClass(), classToReturn);
  }

  @Override
  public <T extends IsIndexed> void checkObjectPermissions(User user, SelectedItems<T> objs)
    throws AuthorizationDeniedException, GenericException, RequestNotValidException {
    checkObjectPermissions(user, objs, null);
  }

  @Override
  public <T extends IsIndexed> void checkObjectPermissions(User user, SelectedItems<T> objs, Class<T> classToReturn)
    throws AuthorizationDeniedException, GenericException, RequestNotValidException {
    UserUtility.checkObjectPermissions(user, objs, requester.getClass(), classToReturn);
  }

  public void setRelatedObjectId(String relatedObjectId) {
    this.relatedObjectId = relatedObjectId;
  }

  public String getRelatedObjectId() {
    return relatedObjectId;
  }

  public void setRelatedAipId(String relatedAipId) {
    this.relatedAipId = relatedAipId;
  }

  public String getRelatedAipId() {
    return relatedAipId;
  }

  public void setParameters(Object... parameters) {
    this.parameters = parameters;
  }

  public Object[] getParameters() {
    return parameters;
  }

  public void addParameters(Object... parameters) {
    if (this.parameters == null) {
      this.parameters = parameters;
    } else {
      ArrayList<Object> paramsList = new ArrayList<>();
      paramsList.addAll(Arrays.asList(this.parameters));
      paramsList.addAll(Arrays.asList(parameters));
      this.parameters = paramsList.toArray();
    }
  }
}
