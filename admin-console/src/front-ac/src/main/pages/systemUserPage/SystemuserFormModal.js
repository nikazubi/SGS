import FormModal from "../../../components/modal/FormModal";
import {useState} from "react";
import SystemUserForm from "./SystemUserForm";
import useCreateSystemuser from "./useCreateSystemuser";
import {ModalOpenMode} from "../../../utils/constants";


const SystemUserModal = ({data, open, news, onClose, modalOpenMode, submitButton, onUpdate, ...props}) => {

    const {mutate: onCreate} = useCreateSystemuser();

    const initialValues = {
        id: data?.id? data.id : 0,
        username: data?.username? data.username : '',
        academyClasses: data?.academyClassList? data.academyClassList : [],
        systemGroup: data?.groups? data.groups : [],
        name: data?.name? data.name : '',
        password: data?.password? data.password : '',
        email: data?.email? data.email : ''
    };

    return (
        <FormModal
            open={open}
            title={"დამატება"}
            cancelText={'დახურვა'}
            saveText={'შენახვა'}
            width={700}
            height={400}
            maxWidth={700}
            maxHeight={400}
            initialValues={

                {
                    ...initialValues
                }
            }
            formProps={{
                modalOpenMode
            }}
            form={SystemUserForm}
            // validationSchema={validateNewsFormData(!!news, t)}
            onSubmit={(options) => modalOpenMode === ModalOpenMode.add ? onCreate(options) : onUpdate(options)}
            onClose={onClose}
            submitButton={submitButton}
            {...props}
        />
    );
};

export default SystemUserModal;