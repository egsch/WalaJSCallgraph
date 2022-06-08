function mySecondFunction(b) {
    return b*2;
}

function myFirstFunction(a) {
    var y = mySecondFunction(5);
    console.log(y);
    return y;
}

myFirstFunction();

/* class MyClass {
    constructor(idk, attribute){
        this.idk = idk;
        this.attribute = attribute;
    }
    myNewMethod (){
        console.log(this.idk + this.attribute);
    }
}

joe = new MyClass("hello ", "world");
joe.myNewMethod(); */