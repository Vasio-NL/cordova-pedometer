var exec = require("cordova/exec");

var Pedometer = function () {
    this.name = "Pedometer";
};

Pedometer.prototype.isStepCountingAvailable = function (onSuccess, onError) {
    exec(onSuccess, onError, "Pedometer", "isStepCountingAvailable", []);
};

Pedometer.prototype.isDistanceAvailable = function (onSuccess, onError) {
    exec(onSuccess, onError, "Pedometer", "isDistanceAvailable", []);
};

Pedometer.prototype.isFloorCountingAvailable = function (onSuccess, onError) {
    exec(onSuccess, onError, "Pedometer", "isFloorCountingAvailable", []);
};

Pedometer.prototype.startPedometerUpdates = function (onSuccess, onError) {
    exec(onSuccess, onError, "Pedometer", "startPedometerUpdates", []);
};

Pedometer.prototype.stopPedometerUpdates = function (onSuccess, onError) {
    exec(onSuccess, onError, "Pedometer", "stopPedometerUpdates", []);
};

Pedometer.prototype.getHistory = function (onSuccess, onError) {
    cordova.exec(function(result){
        var parsedResult = JSON.parse(result);
        onSuccess(parsedResult);
    },onError, "Pedometer", "getHistory", []);
};

module.exports = new Pedometer();
