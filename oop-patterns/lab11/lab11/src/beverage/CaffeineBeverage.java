package beverage;

public abstract class CaffeineBeverage {
    public final void prepareRecipe() {
        boilWater();
        brew();
        pourInCup();
        addCondiments();
    }
    protected void boilWater() {
        System.out.println("Boil some water");
    }
    protected void pourInCup() {
        System.out.println("Pour beverage in cup");
    }
    protected abstract void brew();
    protected abstract void addCondiments();
}
