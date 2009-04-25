package br.com.caelum.vraptor.vraptor2;

import br.com.caelum.vraptor.core.RequestInfo;
import br.com.caelum.vraptor.resource.Resource;
import br.com.caelum.vraptor.resource.ResourceMethod;
import br.com.caelum.vraptor.view.PathResolver;
import br.com.caelum.vraptor.view.ResultException;
import br.com.caelum.vraptor.view.jsp.PageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vraptor.core.WebRequest;
import org.vraptor.http.VRaptorServletRequest;
import org.vraptor.http.VRaptorServletResponse;
import org.vraptor.introspector.ExpressionEvaluationException;
import org.vraptor.introspector.ExpressionEvaluator;
import org.vraptor.scope.DefaultLogicRequest;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ViewsPropertiesPageResult implements PageResult {

    private final Config config;
    private final HttpServletRequest request;
    private final PathResolver resolver;
    private final ResourceMethod method;
    private final HttpServletResponse response;
    private final ExpressionEvaluator evaluator = new ExpressionEvaluator();
    private final DefaultLogicRequest logic;

    private static final Logger logger = LoggerFactory.getLogger(ViewsPropertiesPageResult.class);

    public ViewsPropertiesPageResult(Config config, HttpServletRequest request, PathResolver resolver,
            RequestInfo requestInfo, HttpServletResponse response, ServletContext context) {
        this.config = config;
        this.request = request;
        this.resolver = resolver;
        this.method = requestInfo.getResourceMethod();
        this.response = response;
        logic = new DefaultLogicRequest(null, new WebRequest(new VRaptorServletRequest(request, null),
                new VRaptorServletResponse(response), context), null);
    }

    public void forward(String result) {
        try {
            Resource resource = method.getResource();
            if (!Info.isOldComponent(resource)) {
                request.getRequestDispatcher(resolver.pathFor(method, result)).forward(request, response);
                return;
            }
            String key = Info.getComponentName(resource.getType()) + "."
                    + Info.getLogicName(method.getMethod()) + "." + result;

            String path = config.getForwardFor(key);

            if (path == null) {
                request.getRequestDispatcher(resolver.pathFor(method, result)).forward(request, response);
            } else {
                try {
                    result = evaluator.parseExpression(path, logic);
                } catch (ExpressionEvaluationException e) {
                    throw new ServletException("Unable to redirect while evaluating expression '" + path + "'.", e);
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("overriden view found for " + key + " : " + path + " expressed as " + result);
                }
                if (result.startsWith("redirect:")) {
                    response.sendRedirect(result.substring(9));
                } else {
                    request.getRequestDispatcher(result).forward(request, response);
                }
            }
        } catch (ServletException e) {
            throw new ResultException(e);
        } catch (IOException e) {
            throw new ResultException(e);
        }
    }

    public void include(String result) {
        try {
            request.getRequestDispatcher(resolver.pathFor(method, result)).include(request, response);
        } catch (ServletException e) {
            throw new ResultException(e);
        } catch (IOException e) {
            throw new ResultException(e);
        }
    }

    public void include(String key, Object value) {
        request.setAttribute(key, value);
    }

}
