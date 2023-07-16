import FormModal from "../../../components/modal/FormModal";
import {useState} from "react";
import SystemUserForm from "./SystemUserForm";
import useCreateSystemuser from "./useCreateSystemuser";


const initialValues = {
    username: '',
    academyClasses: [],
    systemGroup: [],
    name: '',
    password: '',
    email: ''
};

const SystemUserModal = ({open, news, onClose, modalOpenMode, submitButton, ...props}) => {
    const {mutate: onCreate} = useCreateSystemuser();
    const [date, setDate] = useState(new Date());

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
                    ...initialValues,
                    ...news,
                }
            }
            formProps={{
                modalOpenMode
            }}
            form={SystemUserForm}
            // validationSchema={validateNewsFormData(!!news, t)}
            onSubmit={onCreate}
            onClose={onClose}
            submitButton={submitButton}
            {...props}
        />
    );
};

export default SystemUserModal;