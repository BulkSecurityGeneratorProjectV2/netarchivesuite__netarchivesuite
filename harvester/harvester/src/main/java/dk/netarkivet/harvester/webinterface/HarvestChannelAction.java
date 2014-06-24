
package dk.netarkivet.harvester.webinterface;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.ForwardedToErrorPage;
import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.common.webinterface.HTMLUtils;
import dk.netarkivet.harvester.datamodel.HarvestChannel;
import dk.netarkivet.harvester.datamodel.HarvestChannelDAO;
import dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO;

/**
 * Abstract class representing web UI action for harvest channels.
 */
public abstract class HarvestChannelAction {
	
	public static enum ActionType {
		createHarvestChannel,
		mapHarvestToChannel
	}
	
	public final static String ACTION = "channelAction";
	public final static String CHANNEL_NAME = "channelName";
	public final static String HARVEST_ID = "harvestId";
	public final static String CHANNEL_ID = "channelId";
	public final static String SNAPSHOT = "channelIsSnapshot";
	public final static String COMMENTS = "channelComments";

    private static final Log log =
            LogFactory.getLog(HarvestChannelAction.class);

    /**
     * This method processes the request to determine which action it
     * corresponds to and passes the request along accordingly. Available actions are:
     * <ul>
     * <li>create harvest channel</li>
     * <li>map harvest definition to channel</li>
     * </ul>
     *
     * @param context the original servlet context of the request.
     * @param i18n the internationalisation to be used.
     * @throws ForwardedToErrorPage if an exception is thrown while carrying
     * out the action.
     */
    public static void processRequest(PageContext context, I18n i18n) 
    		throws ForwardedToErrorPage {
        ArgumentNotValid.checkNotNull(context, "PageContext context");
        ArgumentNotValid.checkNotNull(i18n, "I18n i18n");
        log.debug("Processing request");
        HttpServletRequest request = (HttpServletRequest) context.getRequest();
        try {
        	String action = request.getParameter(ACTION);
        	if (action == null || action.isEmpty()) {
            	return;
            }            
            switch (ActionType.valueOf(action)) {
            	case createHarvestChannel:
            		String name = request.getParameter(CHANNEL_NAME);
            		HTMLUtils.forwardOnEmptyParameter(context, CHANNEL_NAME);
            		HarvestChannelDAO dao = HarvestChannelDAO.getInstance();
            		dao.create(new HarvestChannel(name, false, false, request.getParameter(COMMENTS)));
            		break;
            	case mapHarvestToChannel:
            		long harvestId = Long.parseLong(request.getParameter(HARVEST_ID));
            		long channelId = Long.parseLong(request.getParameter(CHANNEL_ID));
            		HarvestChannelDAO hcDao = HarvestChannelDAO.getInstance();
            		HarvestDefinitionDAO hdDao = HarvestDefinitionDAO.getInstance();
            		hdDao.mapToHarvestChannel(harvestId, hcDao.getById(channelId));
            		break;
            	default: 
            }
        } catch (Throwable e) {
            HTMLUtils.forwardWithErrorMessage(context, i18n, e,
                                         "errormsg;harvest.channel.create.error");
            throw new ForwardedToErrorPage("Error in Harvest Channels", e);
        }
    }

}
