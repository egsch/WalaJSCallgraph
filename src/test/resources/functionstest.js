function mySecondFunction(b) {
    return b*2;
}

function myFirstFunction(a) {
    var y = mySecondFunction(5);
    console.log(y);
    return y;
}

function myThirdFunction(myFunc) {
    myFunc()
}

var arr = [];
var yes = arr.isArray();
myThirdFunction(myFirstFunction);

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