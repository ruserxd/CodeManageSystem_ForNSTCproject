export const emailValidator = (value) => {
    if (value.length <= 0) 
        return { valid: false, errorMsg: "必須填寫" };

    if (!value.match(/^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9-]+(?:\.[a-zA-Z0-9-]+)*$/))
        return { valid: false, errorMsg: "請輸入有效的 email" };

    return {
        valid: true,
        errorMsg: ""
    };
};