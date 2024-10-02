export const accountValidator = (value) => {
    if (value.length <= 0) 
        return { valid: false, errorMsg: "必須填寫" };
    return {
        valid: true,
        errorMsg: ""
    };
};