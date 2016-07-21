console.log('loaded setup.js');

var currentField = new inputField("field");

var submitCandidate = new inputField("submit");

function inputField(candidateType) {

    this.candidateType = candidateType;
    this.fieldId = "";
    this.hasId = false;
    this.fieldName = "";
    this.hasName = false;
    this.value = "";

    this.hasForm = false;
    this.inputType = "";
    this.metaType = "";

    this.tagName = "";

    this.formHasPassword = false;
    this.formHasTooManyPasswords = false;

    this.formTextFieldsCount;
    this.hasAction = false;
    this.formAction = "";
    this.formId = "";
    this.formName = "";
    this.hasButton = false;
    this.buttonInner = "";

    this.populateFromElement = function (domElement) {

        console.log('in populate from element');

        var formNode;

        console.log('----------------------------');

        this.fieldId = jQuery(domElement).attr('id');

        console.log("checking fieldId length");
        console.log(typeof (this.fieldId));

        if (!(typeof (this.fieldId) == "undefined") && (this.fieldId.length > 0)) {

            console.log('has id is true ' + this.fieldId);
            this.hasId = true;
        } else {
            this.fieldId = '#';

        }

        this.fieldName = jQuery(domElement).attr('name');
        if (this.fieldName !== "undefined") {
            console.log('has name is true ' + this.fieldName);
            this.hasName = true;
        } else {
            this.fieldName = "#";

        }

        this.value = jQuery(domElement).prop('value');
        console.log('this value is: ' + this.value);

        console.log("checking for form");


        formNode = jQuery(domElement).closest("form");

        if (jQuery(formNode).length) {
            console.log('has form');
            this.hasForm = true;
        }

        this.tagName = jQuery(domElement).prop('tagName');
        console.log('this tag is ' + this.tagName);


        if (this.tagName == "INPUT") {
            this.inputType = jQuery(domElement).attr('type');
            console.log('input type is: ' + this.inputType);

            if ((this.inputType == 'email') || (this.inputType == 'text') || (this.inputType == 'tel') || (typeof (this.inputType) == 'undefined')) {
                console.log('is meta type T');
                this.metaType = "T";

            } else if (this.inputType == 'password') {

                this.metaType = "P";
            } else if (this.inputType == 'submit') {

                this.metaType = "S";
            }

        } else if (this.tagName.toLowerCase() == "button") {

            var buttonType = jQuery(domElement).attr('type');

            // example: scccounty.bank (2nd page, login)

            console.log('button type submit case');
            this.hasButton = true;
            this.buttonInner = jQuery(domElement).find(':only-child:last').html();
            if (typeof (this.buttonInner) == 'undefined') {
                this.buttonInner = jQuery(domElement).html();

            }
            this.metaType = "B";

        }



        console.log('analyzing nearby text fields for this form');

        if (this.hasForm) {

            var visibleText = jQuery("input[type=text]:visible,input[type=email]:visible,input:not([type]):visible");

            jQuery(visibleText).each(function () {

                console.log("name of field is: " + jQuery(this).prop("name"));

            });

            this.formTextFieldsCount = jQuery(visibleText).length;

            console.log("number of visible text type fields in form " + this.formTextFieldsCount);

            var visiblePassword = jQuery("input[type=password]:visible");
            passwordCount = jQuery(visiblePassword).length;


            if (passwordCount == 1) {

                console.log('form has one password');
                this.formHasPassword = true;
            } else if (passwordCount == 0) {

                console.log('form has no password');
            } else if (passwordCount > 1) {
                this.formHasTooManyPasswords = true;
                console.log('form has too many passsword');
            }


            if ((this.metaType == "S") || (this.metaType == "B")) {
                console.log("testing for action");

                this.formAction = jQuery(formNode).prop("action");

                console.log("got action");


                if (typeof (this.formAction) == 'undefined') {
                    console.log('no form action undefined')


                } else if (this.formAction.length < 1) {

                    console.log('no form action < 1')
                } else {
                    this.hasAction = true;
                }

            }

            console.log("getting form id");


            this.formId = jQuery(formNode).prop("id");


            if ((typeof (this.formId) == "undefined") || (jQuery(this.formId).length < 1)) {

                this.formId = "#";
            }

            console.log("getting form name");
            this.formName = jQuery(formNode).prop("name");
            console.log('form name is: ' + this.formName);

            if ((typeof (this.formName) == "undefined") || (jQuery(this.formName).length < 1)) {

                this.formName = "#";
            }



        }

    };

}



function getLastCurrentField() {

    console.log('----------------------------');
    console.log('calling exit with most recent current field');
    return JSON.stringify(currentField);




}

function exitWithFieldInfo(someField) {

    console.log('----------------------------');
    console.log('about to stringify');

    console.log("exiting for id " + someField.tagName);
    console.log("exiting for name  " + someField.id);


    var fieldInfoString = JSON.stringify(someField);

    console.log("field info as json: " + fieldInfoString);

    Android.returnResult(fieldInfoString);

}

function armForHasAccount() {
    console.log("arming for has account case");
    jQuery("input").click(function () {

        console.log('did click text field in has account ' + jQuery(this).attr('id'));
        currentField.populateFromElement(jQuery(this));


        console.log('stringify populated ' + JSON.stringify(currentField));

        if ((currentField.metaType == "T") || (currentField.metaType == "P")) {
            console.log('calling exit with text type' + currentField.id);
            exitWithFieldInfo(currentField); // this should be count of textfields that are close in this form
        } else {
            console.log('not identified as T or P type field');

        }

    });

    return "did arm for has account";
}

function armForNoAccount() {

    console.log("arming for no account case");

    console.log('attempting to bind change event to input:');

    jQuery("input").change(function () {

        console.log('did click text field in no account ' + jQuery(this).attr('id'));
        currentField.populateFromElement(jQuery(this));

        console.log('stringify populated ' + JSON.stringify(currentField));

        if ((currentField.metaType == "T") || (currentField.metaType == "P")) {
            console.log('calling exit with text type' + currentField.id);
            exitWithFieldInfo(currentField); // this should be count of textfields that are close in this form
        } else {
            console.log('not identified as T or P type field');
        }

    });

    return "did arm for no account";
}

console.log('attempting to bind submit event - use for both has account and no account cases:');

jQuery(":button").click(function (event) {

    console.log("did click button: " + jQuery(this).attr('id'));

    console.log("responding to button click");

    event.preventDefault();

    submitCandidate.populateFromElement(jQuery(this));

    console.log('calling exit with text type' + currentField.id);
    exitWithFieldInfo(submitCandidate); // this should be count of textfields that are close in this form

});

jQuery(":submit").click(function (event) {

    console.log("did click submit button: " + jQuery(this).attr('id'));

    console.log("responding to submit  click");

    if(jQuery(this).attr('id') != 'next') event.preventDefault();

    submitCandidate.populateFromElement(jQuery(this));

    console.log('calling exit with text type' + currentField.id);
    exitWithFieldInfo(submitCandidate); // this should be count of textfields that are close in this form


    // processSubmitOrButtonClick(jQuery(this));

});

function insertValueForAttributeTypeValue(at, av, fieldValue) {

    var selectorType;

    if (at == "N") {

        selectorType = "name";
    } else if (at == "I") {
        selectorType = "id";

    }

    var selector = "[" + selectorType + "=" + av + "]";

    console.log("selector is: " + selector);
    console.log("field value is: " + fieldValue);

    jQuery(selector).val(fieldValue);

    return "inser value did return";
}

console.log('loaded main function');

(function () {
    console.log('did perform setup notification');
    return "did perform setup";
})();