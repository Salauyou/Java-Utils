package locks;


import java.math.BigDecimal;
import java.util.Random;

public class Helper {

    static public void log(Object o) {
        System.out.println("=============== " + o);
    }
    

    final static char[] CHARS = "01234567890qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM".toCharArray();
    
    
    
    public static String generateString(Random rnd, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) 
            sb.append(CHARS[rnd.nextInt(CHARS.length)]);
        return sb.toString();
    }
    
    
    final static char[] CHARS_NUMERIC = "0123456789".toCharArray();
    
    
    public static  String generateStringNumeric(Random rnd, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) 
            sb.append(CHARS_NUMERIC[rnd.nextInt(CHARS_NUMERIC.length)]);
        return sb.toString();
    }
    
    
    public static BigDecimal generateBigDecimal(Random rnd, double min, double max) {
        return BigDecimal.valueOf((int) (min * 100) + rnd.nextInt((int)((max - min) * 100))).scaleByPowerOfTen(-2);
    }
    
    
    final static String[] NAMES = { "Antonio", "Thrall", "Arletta", "Fuentes",
        "Deedra", "Egli", "Kassie", "Greenspan", "Monique", "Garibaldi",
        "Lynsey", "Banks", "Justin", "Fellows", "Alejandra", "Camper",
        "Elenore", "Mendiola", "Merna", "Rochon", "Fredia", "Wang",
        "Jacinto", "Janz", "Justina", "Rothchild", "Waltraud", "Moriarity",
        "Jeanie", "Vitti", "Candace", "Fowles", "Lucius", "Hinojosa",
        "Rhona", "Dent", "Marilou", "Maurice", "Hugh", "Ovitt", "Cristi",
        "Redus", "Lashell", "Asuncion", "Herma", "Dorman", "Mayme",
        "Overland", "Ali", "Teague", "Catherin", "Simon", "Soon",
        "Beckerman", "Randy", "Rempel" };
    
    
    public static String generateName(Random rnd, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) 
            sb.append(NAMES[rnd.nextInt(NAMES.length)]).append(" ");
        return sb.deleteCharAt(sb.length() - 1).toString();
    }
    
}