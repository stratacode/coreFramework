abstract class CServlet extends HttpServlet {

   public abstract StringBuilder output(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException;

   public void service(javax.servlet.http.HttpServletRequest request, javax.servlet.http.HttpServletResponse response) throws IOException, ServletException {
      StringBuilder out = output(request, response);
      response.getWriter().print(out);
   }
}
