package beverage;

public class Tea extends CaffeineBeverage{
    @Override
    protected void brew(){
        System.out.println("Steeping the Tea");
    }

    @Override
    protected void addCondiments(){
        System.out.println("Adding Lemon");
    }
}
