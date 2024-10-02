export const nameValidator = (value) => {
    console.log(value.length);
    if (value.length <= 0) {
        return {valid: false, errorMsg: "必須填寫"};
    }

    if (value.length < 2) {
        return { valid: false, errorMsg: "名稱至少需要 2 個字符" };
    }
    
    if (value.length > 20) {
        return { valid: false, errorMsg: "名稱不能超過 20 個字符" };
    }

    return {
        valid: true,
        errorMsg: ""
    };
};