package ru.salauyou.util.misc;

/**
 * @author Salauyou
 */
public class ExceptionHelper {

    
    static public String buildExceptionMessage(Throwable e) {
        final String join = " <- ";
        StringBuilder sb = new StringBuilder();
        while (e != null) {
            sb.append(e.getClass().getCanonicalName());
            if (e.getMessage() != null)
               sb.append(" (").append(e.getMessage()).append(')');
            e = e.getCause();
            if (e == null)
                return sb.toString();
            sb.append(join);
        }
        return "";
    }

    
    private ExceptionHelper() { }
}
